package org.tedros.fx.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.util.TLoggerUtil;
import org.tedros.util.TedrosFolder;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

/**
 * Jasper report process driven by in-memory data already available at runtime
 * (form entity, DTO list, etc). Unlike {@link TReportProcess}, this class does
 * not require an EJB report controller or {@code ITReportModel}.
 *
 * @param <T> bean type used as Jasper data source rows
 */
public abstract class TRuntimeReportProcess<T> extends TProcess<TResult<String>> {

	private static final Logger LOGGER = TLoggerUtil.getLogger(TRuntimeReportProcess.class);

	protected static final String PARAM_SUBREPORT_DIR = "SUBREPORT_DIR";
	protected static final String PARAM_LOGO = "logo";
	protected static final String REPORT_ORG = "report_org";

	private enum Action {
		EXPORT_PDF, EXPORT_XLS
	}

	private Collection<T> data;
	private Action action;
	private String reportName;
	private String folderPath;
	private String organization;
	private String subReportDir;
	private InputStream logoInputStream;

	protected TRuntimeReportProcess(String reportName) {
		setAutoStart(true);
		this.reportName = reportName;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public void setLogoInputStream(InputStream logoInputStream) {
		this.logoInputStream = logoInputStream;
	}

	public void setSubReportDir(String subReportDir) {
		this.subReportDir = subReportDir;
	}

	public void exportPDF(T bean, String folderPath) {
		exportPDF(bean == null ? List.of() : Collections.singletonList(bean), folderPath);
	}

	public void exportPDF(Collection<T> data, String folderPath) {
		this.data = data;
		this.action = Action.EXPORT_PDF;
		this.folderPath = folderPath != null ? folderPath : TedrosFolder.EXPORT_FOLDER.getFullPath();
	}

	public void exportXLS(T bean, String folderPath) {
		exportXLS(bean == null ? List.of() : Collections.singletonList(bean), folderPath);
	}

	public void exportXLS(Collection<T> data, String folderPath) {
		this.data = data;
		this.action = Action.EXPORT_XLS;
		this.folderPath = folderPath != null ? folderPath : TedrosFolder.EXPORT_FOLDER.getFullPath();
	}

	@Override
	protected TTaskImpl<TResult<String>> createTask() {
		return new TTaskImpl<TResult<String>>() {
			@Override
			public String getServiceNameInfo() {
				return getProcessName();
			}

			@Override
			protected TResult<String> call() {
				try {
					return executeConfiguredAction();
				} catch (Exception e) {
					setException(e);
					LOGGER.error(e.getMessage(), e);
					return new TResult<>(TState.ERROR, e.getMessage());
				}
			}
		};
	}

	/**
	 * Runs the configured PDF/XLS export synchronously. Package-visible for unit
	 * tests (JavaFX {@code Task#call} is protected).
	 */
	TResult<String> executeConfiguredAction() {
		if (action == Action.EXPORT_PDF) {
			return runExportPdf();
		}
		if (action == Action.EXPORT_XLS) {
			return runExportXls();
		}
		return new TResult<>(TState.ERROR, "No export action configured");
	}

	protected String getDestFile() {
		String pattern = "dd-MM-yyyy HH-mm-ss";
		DateFormat df = new SimpleDateFormat(pattern);
		String k = df.format(new Date());
		String ext = action == Action.EXPORT_PDF ? ".pdf" : ".xlsx";
		return folderPath + reportName + " " + k + ext;
	}

	protected TResult<String> runExportPdf() {
		Map<String, Object> params = new HashMap<>(getReportParameters());
		addSubReportDirParam(params);
		addCustomLogoAndOrgParams(params);
		try (InputStream inputStream = getJasperInputStream()) {
			runBeforeExport(data, params);
			String dest = getDestFile();
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data);
			JasperPrint print = JasperFillManager.fillReport(inputStream, params, ds);
			JasperExportManager.exportReportToPdfFile(print, dest);
			runAfterExport(data, params);
			return new TResult<>(TState.SUCCESS, dest, dest);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return new TResult<>(TState.ERROR, e.getMessage());
		} finally {
			closeLogo();
		}
	}

	protected TResult<String> runExportXls() {
		Map<String, Object> params = new HashMap<>(getReportParameters());
		addSubReportDirParam(params);
		addCustomLogoAndOrgParams(params);
		try (InputStream inputStream = getJasperInputStream()) {
			runBeforeExport(data, params);
			String dest = getDestFile();
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data);
			JasperPrint jasperPrint = JasperFillManager.fillReport(inputStream, params, ds);
			JRXlsxExporter exporter = new JRXlsxExporter();
			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(new File(dest)));
			SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
			configuration.setOnePagePerSheet(true);
			configuration.setDetectCellType(true);
			configuration.setCollapseRowSpan(false);
			configuration.setRemoveEmptySpaceBetweenRows(true);
			exporter.setConfiguration(configuration);
			exporter.exportReport();
			runAfterExport(data, params);
			return new TResult<>(TState.SUCCESS, dest, dest);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return new TResult<>(TState.ERROR, e.getMessage());
		} finally {
			closeLogo();
		}
	}

	protected void addSubReportDirParam(Map<String, Object> params) {
		if (this.subReportDir != null) {
			params.putIfAbsent(PARAM_SUBREPORT_DIR, this.subReportDir);
		}
	}

	protected void addCustomLogoAndOrgParams(Map<String, Object> params) {
		if (!params.containsKey(PARAM_LOGO)) {
			if (this.logoInputStream == null) {
				try {
					this.logoInputStream = new FileInputStream(
							new File(TedrosFolder.IMAGES_FOLDER.getFullPath() + "logo-tedros-medium.png"));
				} catch (FileNotFoundException e) {
					TLoggerUtil.error(getClass(), e.getMessage(), e);
				}
			}
			params.put(PARAM_LOGO, this.logoInputStream);
		}
		if (!params.containsKey(REPORT_ORG) && this.organization != null) {
			params.put(REPORT_ORG, this.organization);
		}
	}

	private void closeLogo() {
		if (this.logoInputStream != null) {
			try {
				this.logoInputStream.close();
			} catch (Exception e) {
				LOGGER.warn(e.getMessage());
			}
			this.logoInputStream = null;
		}
	}

	protected void runBeforeExport(Collection<T> data, Map<String, Object> params) {
	}

	protected void runAfterExport(Collection<T> data, Map<String, Object> params) {
	}

	protected abstract InputStream getJasperInputStream();

	protected abstract Map<String, Object> getReportParameters();

	protected Collection<T> getData() {
		return data;
	}

	protected String getFolderPath() {
		return folderPath;
	}
}

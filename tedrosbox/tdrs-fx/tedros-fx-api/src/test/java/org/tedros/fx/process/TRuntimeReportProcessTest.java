package org.tedros.fx.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;

/**
 * Unit tests for runtime Jasper export without EJB/ITReportModel coupling.
 */
public class TRuntimeReportProcessTest {

	static class Bean {
		String name;

		Bean(String name) {
			this.name = name;
		}
	}

	static class StubProcess extends TRuntimeReportProcess<Bean> {
		boolean beforeCalled;
		boolean afterCalled;
		Collection<Bean> beforeData;
		TResult<String> pdfResult = new TResult<>(TState.SUCCESS, "/tmp/ata.pdf", "/tmp/ata.pdf");
		TResult<String> xlsResult = new TResult<>(TState.SUCCESS, "/tmp/ata.xlsx", "/tmp/ata.xlsx");
		RuntimeException toThrow;

		StubProcess() {
			super("Ata Test");
			setAutoStart(false);
		}

		@Override
		protected InputStream getJasperInputStream() {
			return new ByteArrayInputStream(new byte[0]);
		}

		@Override
		protected Map<String, Object> getReportParameters() {
			return new HashMap<>();
		}

		@Override
		protected void runBeforeExport(Collection<Bean> data, Map<String, Object> params) {
			beforeCalled = true;
			beforeData = data;
		}

		@Override
		protected void runAfterExport(Collection<Bean> data, Map<String, Object> params) {
			afterCalled = true;
		}

		@Override
		protected TResult<String> runExportPdf() {
			if (toThrow != null) {
				throw toThrow;
			}
			runBeforeExport(getData(), new HashMap<>());
			runAfterExport(getData(), new HashMap<>());
			return pdfResult;
		}

		@Override
		protected TResult<String> runExportXls() {
			if (toThrow != null) {
				throw toThrow;
			}
			return xlsResult;
		}

		String destFileForTest() {
			return getDestFile();
		}
	}

	@Test
	public void exportPdfNullBeanUsesEmptyCollectionAndConfiguredFolder() throws Exception {
		StubProcess p = new StubProcess();
		p.exportPDF((Bean) null, "C:/exports/");
		assertEquals("C:/exports/", p.getFolderPath());
		assertNotNull(p.getData());
		assertTrue(p.getData().isEmpty());
	}

	@Test
	public void exportPdfSingleBeanWrapsAsSingletonList() {
		StubProcess p = new StubProcess();
		Bean bean = new Bean("meeting");
		p.exportPDF(bean, "C:/exports/");
		assertEquals(1, p.getData().size());
		assertEquals(bean, p.getData().iterator().next());
	}

	@Test
	public void exportPdfCollectionPreservesData() {
		StubProcess p = new StubProcess();
		List<Bean> list = new ArrayList<>();
		list.add(new Bean("a"));
		list.add(new Bean("b"));
		p.exportPDF(list, "C:/exports/");
		assertEquals(2, p.getData().size());
	}

	@Test
	public void executeWithoutActionReturnsError() {
		StubProcess p = new StubProcess();
		TResult<String> result = p.executeConfiguredAction();
		assertEquals(TState.ERROR, result.getState());
		assertEquals("No export action configured", result.getMessage());
	}

	@Test
	public void executePdfRunsHooksAndReturnsPath() {
		StubProcess p = new StubProcess();
		p.exportPDF(new Bean("x"), "C:/exports/");
		TResult<String> result = p.executeConfiguredAction();
		assertEquals(TState.SUCCESS, result.getState());
		assertEquals("/tmp/ata.pdf", result.getValue());
		assertTrue(p.beforeCalled);
		assertTrue(p.afterCalled);
		assertFalse(p.beforeData.isEmpty());
	}

	@Test
	public void executeXlsReturnsPath() {
		StubProcess p = new StubProcess();
		p.exportXLS(new Bean("x"), "C:/exports/");
		TResult<String> result = p.executeConfiguredAction();
		assertEquals(TState.SUCCESS, result.getState());
		assertEquals("/tmp/ata.xlsx", result.getValue());
	}

	@Test
	public void executePropagatesExceptionFromExport() {
		StubProcess p = new StubProcess();
		p.toThrow = new RuntimeException("jasper boom");
		p.exportPDF(new Bean("x"), "C:/exports/");
		try {
			p.executeConfiguredAction();
			org.junit.Assert.fail("expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("jasper boom", e.getMessage());
		}
	}

	@Test
	public void destFileUsesReportNameAndPdfExtension() {
		StubProcess p = new StubProcess();
		p.exportPDF(new Bean("x"), "C:/exports/");
		String dest = p.destFileForTest();
		assertTrue(dest.startsWith("C:/exports/Ata Test "));
		assertTrue(dest.endsWith(".pdf"));
	}

	@Test
	public void destFileUsesXlsxExtension() {
		StubProcess p = new StubProcess();
		p.exportXLS(List.of(new Bean("x")), "C:/exports/");
		String dest = p.destFileForTest();
		assertTrue(dest.endsWith(".xlsx"));
	}
}

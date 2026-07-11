package org.tedros.ai.toolrelay.function.extension.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.extension.ai.model.FileInfo}
 * (app-extensions-fx). A description do id referencia a tool de download
 * pelo nome literal — {@code DownloadFileFunction} permanece no FE
 * (multimodal, fora da Fase 1).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonClassDescription("File information")
public class FileInfo {

	@JsonPropertyDescription("the file id, use this id to download the file from function download_tedros_file")
	private Long id;

	@JsonPropertyDescription("the file name")
	private String fileName;

	@JsonPropertyDescription("the file size in bytes")
	private Long fileSize;

	@JsonPropertyDescription("the file extension")
	private String fileExtension;

	@JsonPropertyDescription("the file owner")
	private String owner;
}

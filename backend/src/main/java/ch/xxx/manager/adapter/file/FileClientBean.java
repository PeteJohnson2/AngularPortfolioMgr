/**
 *    Copyright 2019 Sven Loesekann
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package ch.xxx.manager.adapter.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.xxx.manager.domain.file.FileClient;
import ch.xxx.manager.domain.model.entity.dto.FinancialElementDto;
import ch.xxx.manager.domain.model.entity.dto.SymbolFinancialsDto;
import ch.xxx.manager.usecase.service.AppInfoService;
import ch.xxx.manager.usecase.service.FinancialDataImportService;

@Component
public class FileClientBean implements FileClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileClientBean.class);
	private AppInfoService appInfoService;
	private ObjectMapper objectMapper;
	private FinancialDataImportService financialDataImportService;
	String financialDataImportPath;

	public FileClientBean(AppInfoService appInfoService, ObjectMapper objectMapper,
			FinancialDataImportService financialDataImportService) {
		this.appInfoService = appInfoService;
		this.objectMapper = objectMapper;
		this.financialDataImportService = financialDataImportService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void doOnStartup() {
		this.financialDataImportPath = this.appInfoService.getFinancialDataImportPath();
	}

	public Boolean importZipFile(String filename) {
		ZipFile initialFile = null;
		try {
			initialFile = new ZipFile(this.financialDataImportPath + filename);
			Enumeration<? extends ZipEntry> entries = initialFile.entries();
			LocalDateTime startCleanup = LocalDateTime.now();
			LOGGER.info("Clear start");
			this.financialDataImportService.clearFinancialsData();
			LOGGER.info("Clear time: {}", ChronoUnit.MILLIS.between(startCleanup, LocalDateTime.now()));
			List<SymbolFinancialsDto> symbolFinancialsDtos = new ArrayList<>();
			boolean first = true;
			while (entries.hasMoreElements()) {
				ZipEntry element = entries.nextElement();
				LocalDateTime start = LocalDateTime.now();
				if (!element.isDirectory() && element.getSize() > 10) {
					InputStream inputStream = null;
					try {
						if (first) {
							LOGGER.info("Filename: {}, Filesize: {}", element.getName(), element.getSize());
							first = false;
						}
						inputStream = initialFile.getInputStream(element);
						String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
						SymbolFinancialsDto symbolFinancialsDto = this.objectMapper.readValue(text,
								SymbolFinancialsDto.class);
						Optional.ofNullable(symbolFinancialsDto.getData()).stream().forEach(myFinancialsDataDto -> {
							myFinancialsDataDto.setBalanceSheet(myFinancialsDataDto.getBalanceSheet().stream()
									.map(myFinancialElementDto -> fixConcept(myFinancialElementDto))
									.collect(Collectors.toSet()));
							myFinancialsDataDto.setCashFlow(myFinancialsDataDto.getCashFlow().stream()
									.map(myFinancialElementDto -> fixConcept(myFinancialElementDto))
									.collect(Collectors.toSet()));
							myFinancialsDataDto.setIncome(myFinancialsDataDto.getIncome().stream()
									.map(myFinancialElementDto -> fixConcept(myFinancialElementDto))
									.collect(Collectors.toSet()));
						});
						symbolFinancialsDtos.add(symbolFinancialsDto);
//						LOGGER.info(symbolFinancialsDto.toString());
//						LOGGER.info(text != null ? text.substring(0, 100) : "");
					} catch (Exception e) {
						LOGGER.info("Exception with file: {}", element.getName(), e);
					} finally {
						inputStream.close();
					}
				}
				if (symbolFinancialsDtos.size() >= 500 || !entries.hasMoreElements()) {
					this.financialDataImportService.storeFinancialsData(symbolFinancialsDtos);
					symbolFinancialsDtos.clear();
					LOGGER.info("Persist time: {}", ChronoUnit.MILLIS.between(start, LocalDateTime.now()));
					first = true;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			this.closeFile(initialFile);
		}
		return true;
	}

	private FinancialElementDto fixConcept(FinancialElementDto myFinancialElementDto) {
		myFinancialElementDto.setConcept(
				myFinancialElementDto.getConcept() != null && myFinancialElementDto.getConcept().contains(":")
						? myFinancialElementDto.getConcept().trim()
								.substring(myFinancialElementDto.getConcept().indexOf(':') + 1)
						: myFinancialElementDto.getConcept());
		return myFinancialElementDto;
	}

	private void closeFile(ZipFile zipFile) {
		if (zipFile != null) {
			try {
				zipFile.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}

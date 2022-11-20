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
package ch.xxx.manager.domain.model.entity;

import java.util.List;
import java.util.Optional;

import ch.xxx.manager.domain.model.dto.FeConceptDto;

public interface FinancialElementRepository {
	FinancialElement save(FinancialElement financialElement);
    List<FinancialElement> saveAll(Iterable<FinancialElement> symbols);
	Optional<FinancialElement> findById(Long id);
	void deleteAllBatch();
	void dropSymbolFinancialsIdIndex();
	void dropConceptIndex();
	void createSymbolFinancialsIdIndex();
	void createConceptIndex();
	List<FeConceptDto> findCommonFeConcepts();
}

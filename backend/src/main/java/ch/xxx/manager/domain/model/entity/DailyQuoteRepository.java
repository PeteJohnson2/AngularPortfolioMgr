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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface DailyQuoteRepository {
	List<DailyQuote> findBySymbol(String symbol);

	Set<DailyQuote> findBySymbolId(Long symbolId);
	List<DailyQuote> findBySymbolId(Long symbolId, LocalDate start, LocalDate end);
	List<DailyQuote> findBySymbolIds(List<Long> symbolId);
	List<DailyQuote> findBySymbolKeys(List<String> symbolKeys);

	List<DailyQuote> findBySymbolAndDayBetween(String symbol, LocalDate start, LocalDate end);
	List<DailyQuote> saveAll(List<DailyQuote> dailyquotes);
	DailyQuote save(DailyQuote dailyQuote);
	void delete(DailyQuote dailyQuote);
	void deleteAll(Iterable<DailyQuote> dailyQuotes);
}

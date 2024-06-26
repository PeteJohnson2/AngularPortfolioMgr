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
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, of } from "rxjs";
import { tap } from "rxjs/operators";
import { QuarterData } from "../model/quarter-data";
import { FeConcept } from "../model/fe-concept";
import { SymbolFinancialsQueryParams } from "../model/symbol-financials-query-params";
import { SymbolFinancials } from "../model/symbol-financials";
import { FeCountry } from "../model/fe-country";
import { SfSymbolName } from "../model/sf-symbol-name";
import { FeIdInfo } from "../model/fe-id-info";
import { ImportData } from "src/app/model/import-data";

@Injectable()
export class FinancialDataService {
  private quarters: QuarterData[] = [];
  private feConcepts: FeConcept[] = [];
  private feCountries: FeCountry[] = [];
  private sfSymbolNames: SfSymbolName[] = [];

  constructor(private http: HttpClient) {}

  putImportFinancialsData(
    importFinancialsData: ImportData,
  ): Observable<string> {
    return this.http.put<string>(
      "/rest/financialdata/importus/data",
      importFinancialsData,
      { responseType: "json" },
    );
  }

  getQuarters(): Observable<QuarterData[]> {
    if (this.quarters.length > 0) {
      return of(this.quarters);
    } else {
      return this.http
        .get<QuarterData[]>("/rest/financialdata/symbolfinancials/quarters/all")
        .pipe(tap((values) => (this.quarters = values)));
    }
  }

  getConcepts(): Observable<FeConcept[]> {
    if (this.feConcepts.length > 0) {
      return of(this.feConcepts);
    } else {
      return this.http
        .get<FeConcept[]>(`/rest/financialdata/financialelement/concept/all`)
        .pipe(tap((values) => (this.feConcepts = values)));
    }
  }

  getFeInfo(id: number): Observable<FeIdInfo> {
    return this.http.get<FeIdInfo>(
      `/rest/financialdata/financialelement/id/${id}`,
    );
  }

  getSymbolNamesBySymbol(symbol: string): Observable<SfSymbolName[]> {
    return this.http.get<SfSymbolName[]>(
      `/rest/financialdata/symbolfinancials/symbol/${symbol}`,
    );
  }

  getSymbolNamesByCompanyName(symbol: string): Observable<SfSymbolName[]> {
    return this.http.get<SfSymbolName[]>(
      `/rest/financialdata/symbolfinancials/companyname/${symbol}`,
    );
  }

  getCountries(): Observable<FeCountry[]> {
    if (this.feCountries.length > 0) {
      return of(this.feCountries);
    } else {
      return this.http
        .get<FeCountry[]>(`/rest/financialdata/symbolfinancials/countries/all`)
        .pipe(tap((values) => (this.feCountries = values)));
    }
  }

  postSymbolFinancialsParam(
    symbolFinancialsQueryParams: SymbolFinancialsQueryParams,
  ): Observable<SymbolFinancials[]> {
    return this.http.post<SymbolFinancials[]>(
      "/rest/financialdata/search/params",
      symbolFinancialsQueryParams,
    );
  }
}

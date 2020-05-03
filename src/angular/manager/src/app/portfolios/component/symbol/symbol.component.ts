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
import { Component, Input, Output, EventEmitter, OnInit, Inject } from '@angular/core';
import { Symbol } from '../../model/symbol';
import { QuoteService } from '../../service/quote.service';
import { Quote } from '../../model/quote';
import { tap } from 'rxjs/operators';
import { DOCUMENT } from '@angular/common';

const enum QuotePeriodKey { Day, Month, Months3, Months6, Year, Year3, Year5, Year10 }

interface QuotePeriod {
	periodText: string;
	quotePeriodKey: QuotePeriodKey;
}

interface SymbolData {
	start: Date,
	end: Date,
	open: number;
	high: number;
	low: number;
	close: number;
	avgVolume: number;
}

@Component({
	selector: 'app-symbol',
	templateUrl: './symbol.component.html',
	styleUrls: ['./symbol.component.scss']
})
export class SymbolComponent implements OnInit {
	quotePeriods: QuotePeriod[] = [];
	selQuotePeriod: QuotePeriod = null;
	private localSymbol: Symbol;
	quotes: Quote[] = [];
	symbolData = {avgVolume: null, close: null, end: null, high: null, low: null, open: null, start: null} as SymbolData;
	@Output()
	loadingData = new EventEmitter<boolean>();

	constructor(private quoteService: QuoteService, @Inject(DOCUMENT) private document: Document) { }

	ngOnInit(): void {
		this.quotePeriods = [{ quotePeriodKey: QuotePeriodKey.Day, periodText: this.document.getElementById('intraDay').textContent },
		{ quotePeriodKey: QuotePeriodKey.Month, periodText: this.document.getElementById('oneMonth').textContent },
		{ quotePeriodKey: QuotePeriodKey.Months3, periodText: this.document.getElementById('threeMonths').textContent },
		{ quotePeriodKey: QuotePeriodKey.Months6, periodText: this.document.getElementById('sixMonths').textContent },
		{ quotePeriodKey: QuotePeriodKey.Year, periodText: this.document.getElementById('oneYear').textContent },
		{ quotePeriodKey: QuotePeriodKey.Year3, periodText: this.document.getElementById('threeYears').textContent },
		{ quotePeriodKey: QuotePeriodKey.Year5, periodText: this.document.getElementById('fiveYears').textContent },
		{ quotePeriodKey: QuotePeriodKey.Year10, periodText: this.document.getElementById('tenYears').textContent }];
		this.selQuotePeriod = this.quotePeriods[0];
	}

	quotePeriodChanged() {
		this.updateQuotes(this.selQuotePeriod.quotePeriodKey);
		console.log(this.selQuotePeriod);
	}

    private updateSymbolData(startDate: Date, endDate: Date) {
		this.symbolData.start = startDate;
		this.symbolData.end = endDate;
		this.symbolData.open = this.quotes && this.quotes.length > 0 ? this.quotes[0].open : null;
		this.symbolData.close = this.quotes && this.quotes.length > 0 ? this.quotes[this.quotes.length -1].close : null;
		this.symbolData.high = this.quotes && this.quotes.length > 0 ? Math.max(...this.quotes.map(quote => quote.high)) : null;
		this.symbolData.low = this.quotes && this.quotes.length > 0 ? Math.min(...this.quotes.map(quote => quote.low)) : null;
		this.symbolData.avgVolume = this.quotes && this.quotes.length > 0 ? 
			(this.quotes.map(quote => quote.volume).reduce((result, volume) => result + volume, 0) / this.quotes.length) : null;
    }

	private updateQuotes(selPeriod: QuotePeriodKey): void {
		if (!this.symbol) { return; }
		if (selPeriod === QuotePeriodKey.Day) {
			this.loadingData.emit(true);
			this.quoteService.getIntraDayQuotes(this.symbol.symbol)
				.subscribe(myQuotes => {
					this.quotes = myQuotes;
					this.updateSymbolData(new Date(), new Date());
					this.loadingData.emit(false);
				});
		} else {
			this.loadingData.emit(true);
			const startDate = this.createStartDate(selPeriod);			
			const endDate = new Date();
			/*this.quoteService.getAllDailyQuotes(this.symbol.symbol)
				.subscribe(myQuotes => {
					this.quotes = myQuotes;
					this.loadingData.emit(false);
				});*/
			this.quoteService.getDailyQuotesFromStartToEnd(this.symbol.symbol, startDate, endDate)
				.subscribe(myQuotes => {
					this.quotes = myQuotes;
					this.updateSymbolData(startDate, endDate);
					this.loadingData.emit(false);
				});
		}
	}

	private createStartDate(selPeriod: QuotePeriodKey): Date {
		const startDate = new Date();
		if(QuotePeriodKey.Month === selPeriod) {
			startDate.setMonth(startDate.getMonth() - 1);
		} else if(QuotePeriodKey.Months3 === selPeriod) {
			startDate.setMonth(startDate.getMonth() - 3);
		} else if(QuotePeriodKey.Months6 === selPeriod) {
			startDate.setMonth(startDate.getMonth() - 6);
		} else if(QuotePeriodKey.Year === selPeriod) {
			startDate.setMonth(startDate.getMonth() - 12);
		} else if(QuotePeriodKey.Year3 === selPeriod) {
			startDate.setMonth(startDate.getMonth() - 36);
		} else if(QuotePeriodKey.Year5 === selPeriod) {
			startDate.setMonth(startDate.getMonth() - 60);
		} else if(QuotePeriodKey.Year10 === selPeriod) {
			startDate.setMonth(startDate.getMonth() - 120);
		}
		return startDate;
	}

	@Input()
	set symbol(mySymbol: Symbol) {
		if (mySymbol) {
			this.localSymbol = mySymbol;
			this.updateQuotes(!this.selQuotePeriod ? QuotePeriodKey.Day : this.selQuotePeriod.quotePeriodKey);
		}
	}

	get symbol(): Symbol {
		return this.localSymbol;
	}
}

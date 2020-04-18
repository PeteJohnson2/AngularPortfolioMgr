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
import { Component, OnInit, Inject } from '@angular/core';
import { FormGroup, FormBuilder, AbstractControlOptions, FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { OverviewComponent } from '../overview/overview.component';
import { PortfolioData } from '../../model/portfolio-data';
import { Portfolio } from '../../model/portfolio';
import { Symbol } from '../../model/symbol';
import { SymbolService } from '../../service/symbol.service';
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap, switchMap, map } from 'rxjs/operators';

@Component({
  selector: 'app-add-symbol',
  templateUrl: './add-symbol.component.html',
  styleUrls: ['./add-symbol.component.scss']
})
export class AddSymbolComponent implements OnInit {
  private portfolio: Portfolio = null;
  symbolForm: FormGroup;  
  selSymbol: Symbol = null;
  symbolsName: Observable<Symbol[]> = of([]);
  symbolsSymbol: Observable<Symbol[]> = of([]);
  loading = false;
  formValid = true;
  symbolNameFormControl = new FormControl();
  symbolSymbolFormControl = new FormControl();

  constructor(public dialogRef: MatDialogRef<OverviewComponent>,
		@Inject(MAT_DIALOG_DATA) public data: PortfolioData,
		private symbolService: SymbolService,
		private fb: FormBuilder) { 
			this.symbolForm = fb.group({
				symbolSymbol: '',	
				symbolName: ''
			}, {
				validators: [this.validate]
			} as AbstractControlOptions);
			this.portfolio = data.portfolio;
  }

  ngOnInit() {	
	this.symbolsName = this.symbolNameFormControl.valueChanges.pipe(
		debounceTime( 400 ),
        distinctUntilChanged(),
        tap(() => this.loading = true ),
        switchMap( name => name && name.length > 2 ? this.symbolService.getSymbolByName( name )
			.pipe(map(localSymbols => this.filterPortfolioSymbols(localSymbols))) : this.clearSymbol()),
        tap(() => this.loading = false )
	);
	this.symbolsSymbol = this.symbolSymbolFormControl.valueChanges.pipe(
		debounceTime( 400 ),
        distinctUntilChanged(),
        tap(() => this.loading = true ),
        switchMap( name => name && name.length > 2 ? this.symbolService.getSymbolBySymbol( name )
			.pipe(map(localSymbols => this.filterPortfolioSymbols(localSymbols))) : this.clearSymbol()),
        tap(() => this.loading = false )
	);
  }

  private filterPortfolioSymbols(symbols: Symbol[]): Symbol[] {
	return symbols.filter(symbol => this.portfolio.symbols.filter(mySymbol => symbol.symbol === mySymbol.symbol));
  }

  private clearSymbol(): Observable<Symbol[]> {
	this.selSymbol = null;
	return of([]) as Observable<Symbol[]>;
  }

  findSymbolByName() {
	this.symbolService.getSymbolByName(this.symbolNameFormControl.value)
		.subscribe(mySymbols => this.selSymbol = mySymbols.length === 1 ? mySymbols[0] : null);
  }

  findSymbolBySymbol() {
	this.symbolService.getSymbolBySymbol(this.symbolSymbolFormControl.value)
		.subscribe(mySymbols => this.selSymbol = mySymbols.length === 1 ? mySymbols[0] : null);
  }

  onAddClick(): void {
	this.dialogRef.close(this.selSymbol);		
  }

  onCancelClick(): void {
	this.dialogRef.close();
  }

  validate(formGroup: FormGroup) {
	/*
	if (formGroup.get('portfolioName').touched) {
		const myValue: string = formGroup.get('portfolioName').value;
		if(myValue && myValue.trim().length > 4) {
			formGroup.get('portfolioName').setErrors(null);
			this.formValid = true;
		} else {
			formGroup.get('portfolioName').setErrors({ MatchPassword: true });
			this.formValid = false;			
		}
	}
	return { MatchPassword: true } as ValidationErrors;
	return null;
	*/
  }

}

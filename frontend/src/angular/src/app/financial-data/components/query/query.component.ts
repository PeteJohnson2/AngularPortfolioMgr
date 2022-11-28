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
import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import {FinancialsDataUtils, ItemType} from '../../model/financials-data-utils';
import { FormArray, FormGroup, FormBuilder, AbstractControlOptions, Validators, ValidationErrors } from '@angular/forms';

enum FormFields {
	ConceptOperator = 'conceptOperator',
	Concept = 'concept',
	NumberOperator = 'numberOperator',
	NumberValue = 'numberValue'
}

@Component({
  selector: 'app-query',
  templateUrl: './query.component.html',
  styleUrls: ['./query.component.scss']
})
export class QueryComponent implements OnInit, OnDestroy {
  @Input()
  public baseFormArray: FormArray; 
  @Input()
  public formArrayIndex: number;
  @Input()
  public itemType: ItemType; 
  protected termQueryItems = ['And', 'AndNot', 'Or', 'OrNot'];
  protected stringQueryItems: string[] =  ['=', '=*', '*=', '*=*'];
  protected numberQueryItems: string[] =  ['=', '>=', '<='];
  protected readonly conceptsInit: string[] = ['AAA','BBB','CCC'];   
  protected concepts: string[] = [];
  protected FormFields = FormFields;
  protected itemFormGroup: FormGroup;
  
	
  constructor(private fb: FormBuilder) { 
			this.itemFormGroup = fb.group({
				[FormFields.ConceptOperator]: this.stringQueryItems[0],
				[FormFields.Concept]: [this.conceptsInit[0], [Validators.required]],
				[FormFields.NumberOperator]: this.numberQueryItems[0],
				[FormFields.NumberValue]: [0, [Validators.required, Validators.pattern('^[+-]?(\\d+[\\,\\.])*\\d+$')]]
			}
			/*
			, {
				validators: [this.validate]
			} 
			as AbstractControlOptions
			*/
			);
			this.baseFormArray.insert(this.formArrayIndex ,this.itemFormGroup);
			
	}

  ngOnInit(): void {
	this.conceptsInit.forEach(myConcept => this.concepts.push(myConcept));
	this.itemFormGroup.controls[FormFields.Concept].valueChanges.subscribe(myValue => 
	   this.concepts = this.conceptsInit.filter(myConcept => myConcept.includes(myValue)));	
  }
  
  ngOnDestroy(): void {
	this.baseFormArray.removeAt(this.formArrayIndex);
  }
}

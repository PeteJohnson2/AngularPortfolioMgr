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
import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  Output,
  EventEmitter,
  DestroyRef,
  inject,
} from "@angular/core";
import {
  FinancialsDataUtils,
  ItemType,
} from "../../model/financials-data-utils";
import { FormArray, FormGroup, FormBuilder, Validators } from "@angular/forms";
import { debounceTime } from "rxjs/operators";
import { ConfigService } from "src/app/service/config.service";
import { FinancialDataService } from "../../service/financial-data.service";
import { FeConcept } from "../../model/fe-concept";
import { takeUntilDestroyed } from "src/app/base/utils/funtions";

export enum QueryFormFields {
  QueryOperator = "queryOperator",
  ConceptOperator = "conceptOperator",
  Concept = "concept",
  NumberOperator = "numberOperator",
  NumberValue = "numberValue",
  ItemType = "itemType",
}

@Component({
    selector: "app-query",
    templateUrl: "./query.component.html",
    styleUrls: ["./query.component.scss"],
    standalone: false
})
export class QueryComponent implements OnInit {
  protected readonly containsOperator = "*=*";
  @Input()
  public baseFormArray: FormArray;
  @Input()
  public formArrayIndex: number;
  @Input()
  public queryItemType: ItemType;
  @Output()
  public removeItem = new EventEmitter<number>();
  private _showType: boolean;
  private timeoutRef = null;
  protected termQueryItems: string[] = [];
  protected stringQueryItems: string[] = [];
  protected numberQueryItems: string[] = [];
  protected concepts: FeConcept[] = [];
  protected QueryFormFields = QueryFormFields;
  protected itemFormGroup: FormGroup;
  protected ItemType = ItemType;

  constructor(
    private fb: FormBuilder,
    private configService: ConfigService,
    private financialDataService: FinancialDataService,
    private destroyRef: DestroyRef,
  ) {
    this.destroyRef.onDestroy(() => {
      if (!this.showType) {
        this.baseFormArray.removeAt(this.formArrayIndex);
      }
    });
    this.itemFormGroup = fb.group({
      [QueryFormFields.QueryOperator]: "",
      [QueryFormFields.ConceptOperator]: "",
      [QueryFormFields.Concept]: ["", [Validators.required]],
      [QueryFormFields.NumberOperator]: "",
      [QueryFormFields.NumberValue]: [
        0,
        [Validators.required, Validators.pattern("^[+-]?(\\d+[\\,\\.])*\\d+$")],
      ],
      [QueryFormFields.ItemType]: ItemType.Query,
    });
  }

  ngOnInit(): void {
    this.itemFormGroup.controls[QueryFormFields.Concept].valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef), debounceTime(200))
      .subscribe((myValue) =>
        this.financialDataService
          .getConcepts()
          .subscribe(
            (myConceptList) =>
              (this.concepts = myConceptList.filter((myConcept) =>
                FinancialsDataUtils.compareStrings(
                  myConcept.concept,
                  myValue,
                  this.itemFormGroup.controls[QueryFormFields.ConceptOperator]
                    .value,
                ),
              )),
          ),
      );
    this.itemFormGroup.controls[QueryFormFields.ItemType].patchValue(
      this.queryItemType,
    );
    if (
      this.queryItemType === ItemType.TermStart ||
      this.queryItemType === ItemType.TermEnd
    ) {
      this.itemFormGroup.controls[QueryFormFields.ConceptOperator].patchValue(
        this.containsOperator,
      );
      this.itemFormGroup.controls[QueryFormFields.Concept].patchValue("xxx");
      this.itemFormGroup.controls[QueryFormFields.NumberOperator].patchValue(
        "=",
      );
      this.itemFormGroup.controls[QueryFormFields.NumberValue].patchValue(0);
    }
    //make service caching work
    if (this.formArrayIndex === 0) {
      this.getOperators(0);
    } else {
      this.getOperators(400);
    }
  }

  private getOperators(delayMillis: number): void {
    if (!!this.timeoutRef) {
      clearTimeout(this.timeoutRef);
    }
    this.timeoutRef = setTimeout(() => {
      this.financialDataService
        .getConcepts()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe
        //myValues => console.log(myValues)
        ();
      this.configService
        .getNumberOperators()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((values) => {
          this.numberQueryItems = values;
          this.itemFormGroup.controls[
            QueryFormFields.NumberOperator
          ].patchValue(values.filter((myValue) => "=" === myValue)[0]);
        });
      this.configService
        .getStringOperators()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((values) => {
          this.stringQueryItems = values;
          this.itemFormGroup.controls[
            QueryFormFields.ConceptOperator
          ].patchValue(
            values.filter((myValue) => this.containsOperator === myValue)[0],
          );
        });
      if (ItemType.TermEnd === this.queryItemType) {
        this.itemFormGroup.controls[QueryFormFields.QueryOperator].patchValue(
          "End",
        );
        this.itemFormGroup.controls[QueryFormFields.QueryOperator].disable();
      } else {
        this.configService
          .getQueryOperators()
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((values) => {
            this.termQueryItems = values;
            this.itemFormGroup.controls[
              QueryFormFields.QueryOperator
            ].patchValue(values.filter((myValue) => "And" === myValue)[0]);
          });
      }
    }, delayMillis);
  }

  itemRemove(): void {
    this.removeItem.emit(this.formArrayIndex);
  }

  get showType(): boolean {
    return this._showType;
  }

  @Input()
  set showType(showType: boolean) {
    this._showType = showType;
    if (!this.showType) {
      const formIndex =
        this?.baseFormArray?.controls?.findIndex(
          (myControl) => myControl === this.itemFormGroup,
        ) || -1;
      if (formIndex < 0) {
        console.log("showType showType(...)");
        this.baseFormArray.insert(this.formArrayIndex, this.itemFormGroup);
      }
    } else {
      const formIndex =
        this?.baseFormArray?.controls?.findIndex(
          (myControl) => myControl === this.itemFormGroup,
        ) || -1;
      if (formIndex >= 0) {
        console.log("showType remove showType(...)");
        this.baseFormArray.removeAt(formIndex);
      }
    }
  }
}

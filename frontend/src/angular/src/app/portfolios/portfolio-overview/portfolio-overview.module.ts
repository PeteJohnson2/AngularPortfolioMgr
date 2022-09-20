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
import { NgModule } from '@angular/core';
import { BaseModule } from '../../base/base.module';
import { NgxBarChartsModule } from 'ngx-simple-charts/bar';
import { NgxDonutChartsModule } from 'ngx-simple-charts/donut';
import { PortfolioComparisonComponent } from './components/portfolio-comparison/portfolio-comparison.component';
import { PortfolioSectorsComponent } from './components/portfolio-sectors/portfolio-sectors.component';
import { PortfolioChartsComponent } from './components/portfolio-charts/portfolio-charts.component';
import { MatTabsModule } from '@angular/material/tabs'; 
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { PortfoliosOverviewRoutingModule } from './portfolio-overview-routing.module';

@NgModule({
  declarations: [PortfolioChartsComponent, PortfolioComparisonComponent, PortfolioSectorsComponent],
  imports: [
    BaseModule, NgxBarChartsModule, NgxDonutChartsModule, MatTabsModule,MatIconModule,  
    MatProgressSpinnerModule, MatRadioModule, MatCheckboxModule, PortfoliosOverviewRoutingModule
  ]
})
export class PortfolioOverviewModule { }
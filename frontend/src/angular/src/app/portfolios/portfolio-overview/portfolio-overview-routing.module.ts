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
import { NgModule } from "@angular/core";
import { Routes, RouterModule } from "@angular/router";
import { PortfolioChartsComponent } from "./components/portfolio-charts/portfolio-charts.component";
import { NewsListComponent } from "./components/news-list/news-list.component";

const routes: Routes = [
  {
    path: "portfolio-charts/:portfolioId",
    component: PortfolioChartsComponent,
  },
  {
    path: "news-list",
    component: NewsListComponent,
  },
  { path: "**", redirectTo: "portfolio-charts" },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PortfoliosOverviewRoutingModule {}

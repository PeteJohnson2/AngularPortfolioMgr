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
import { Component, Input, OnInit } from "@angular/core";
import { NewsService } from "../../service/news.service";
import {
  Observable,
  ReplaySubject,
  Subject,
  interval,
  mergeMap,
  of,
  repeat,
} from "rxjs";
import { takeUntilDestroyed } from "src/app/base/utils/funtions";
import { NewsItem } from "../../model/news-item";

@Component({
    selector: "app-news-list",
    templateUrl: "./news-list.component.html",
    styleUrl: "./news-list.component.scss",
    standalone: false
})
export class NewsListComponent {
  @Input()
  protected newsItems: NewsItem[] = [];
}

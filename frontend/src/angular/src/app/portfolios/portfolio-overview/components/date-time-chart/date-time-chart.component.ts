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
import { Component, Inject, Input, LOCALE_ID, OnInit } from '@angular/core';
import { DateTime, Duration, Interval } from 'luxon';
import { Item } from '../../model/item';
import { CalendarService } from '../../service/calendar.service';

@Component({
  selector: 'app-date-time-chart',
  templateUrl: './date-time-chart.component.html',
  styleUrls: ['./date-time-chart.component.scss']
})
export class DateTimeChartComponent implements OnInit {	
	private localItems: Item<Event>[] = [];	
	private localStart: Date = new Date();
	private localShowDays: boolean;
	protected end: Date;
	protected dayPx = 0;	
	protected periodDays: DateTime[] = [];
	protected periodMonths: DateTime[] = [];
	protected periodYears: DateTime[] = [];
	protected monthHeaderAnchorIds: string[] = [];
	protected yearHeaderAnchorIds: string[] = [];
	protected anchoreIdIndex = 0;
	protected nextAnchorId = '';
	protected readonly DAY_WIDTH = CalendarService.DAY_WIDTH;
	protected readonly MONTH_WIDTH = CalendarService.MONTH_WIDTH;
	
	constructor(protected calendarService: CalendarService, @Inject(LOCALE_ID) private locale: string) {}
	
    ngOnInit(): void {
		this.calcChartTime();
    }        
    
    protected calcStartPx(item: Item<Event>): number {
		const chartStart = DateTime.fromObject({year: this.start.getFullYear(), month: (!this.showDays ? 1 : this.start.getMonth()+1), day: 1});
		const itemInterval = Interval.fromDateTimes(chartStart, !!item.start ? DateTime.fromJSDate(item.start) : chartStart);
		const itemPeriods = !this.showDays ? itemInterval.length('months') : itemInterval.length('days');
		const result = itemPeriods * ((!this.showDays ? this.MONTH_WIDTH : this.DAY_WIDTH) + 2);
		return result;
	}
	
	protected calcEndPx(item: Item<Event>): number {
		if(!item?.end) {
			return 0;
		}
		const chartEnd = DateTime.fromJSDate(this.end);		
		const itemInterval = Interval.fromDateTimes(DateTime.fromJSDate(item.end), chartEnd);
		const itemPeriods = !this.showDays ? itemInterval.length('months') : itemInterval.length('days');
		const result = itemPeriods * ((!this.showDays ? this.MONTH_WIDTH : this.DAY_WIDTH) + 2);		
		return result;
	}	
	
	protected calcWidthPx(item: Item<Event>): number {
		const chartStart = DateTime.fromObject({year: this.start.getFullYear(), month: !this.showDays ? 1 : this.start.getMonth()+1, day: 1});		
		const chartEnd = DateTime.fromJSDate(this.end);				
		const itemInterval = Interval.fromDateTimes(chartStart, chartEnd);		
		const itemPeriods = !this.showDays ? itemInterval.length('months') : Math.ceil(itemInterval.length('days')); //Math.ceil() for full days 
		//console.log(itemDays * (this.DAY_WIDTH + 2));
		//console.log(itemDays);		
		const result = (itemPeriods * ((!this.showDays ? this.MONTH_WIDTH : this.DAY_WIDTH) + 2) -2) - (this.calcStartPx(item) + this.calcEndPx(item));
		return result;
	}
	
	protected generateHeaderAnchorId(dateTime: DateTime): string {
		const headerAnchorId = '' + dateTime.year + '_' + dateTime.month + '_' + new Date().getMilliseconds().toString(16);		
		return headerAnchorId;
	}
	
	protected scrollToTime(timeDiff: number): void {
		const anchorIds = !this.showDays ? this.yearHeaderAnchorIds : this.monthHeaderAnchorIds; 
		this.anchoreIdIndex = this.anchoreIdIndex + timeDiff < 0 ? 0 : this.anchoreIdIndex + timeDiff >= anchorIds.length ?  
			anchorIds.length -1 : this.anchoreIdIndex + timeDiff;
		this.scrollToAnchorId(anchorIds[this.anchoreIdIndex]);  
	}
	
	protected scrollToAnchorId(anchorId: string): void {
		 const element = document.getElementById(anchorId);
         element.scrollIntoView({
          block: 'start',
          behavior: 'smooth',
          inline: 'center'
        });
	}
	
	private calcChartTime(): void {		
		const myEndOfYear = new Date(new Date().getFullYear(),11,31,23,59,59);
		const endOfYear = DateTime.fromJSDate(myEndOfYear).setLocale(this.locale).setZone(Intl.DateTimeFormat().resolvedOptions().timeZone).toJSDate();		
		let myItem = new Item<Event>();
		myItem.end = DateTime.fromJSDate( new Date(0, 11,31)).setLocale(this.locale).setZone(Intl.DateTimeFormat().resolvedOptions().timeZone).toJSDate();
        const lastEndItem = this.items.reduce((acc, newItem) => acc.end.getMilliseconds() < newItem?.end.getMilliseconds() ? newItem: acc, myItem);
        const openEndItems =  this.items.filter(newItem => !newItem?.end);
        this.end = openEndItems.length > 0 || !this.showDays ? endOfYear : lastEndItem.end.getFullYear() < 1 ? endOfYear : lastEndItem.end;       
        this.periodDays = [];       
        for(let myDay = DateTime.fromObject({year: this.start.getFullYear(), month: this.start.getMonth()+1, day: 1});
        	myDay.toMillis() <= DateTime.fromJSDate(this.end).toMillis();
        	myDay = myDay.plus({days: 1})) {
			this.periodDays.push(myDay);			
		}
		this.periodMonths = [];     
		this.monthHeaderAnchorIds = [];   
		for(let myMonth = DateTime.fromObject({year: this.start.getFullYear(), month: !!this.showDays ? this.start.getMonth()+1 : 1, day: 1}); 
			myMonth.toMillis() <= DateTime.fromJSDate(this.end).toMillis(); 
			myMonth = myMonth.plus({months: 1})) {
			this.periodMonths.push(myMonth);			
			this.monthHeaderAnchorIds.push('M_'+this.generateHeaderAnchorId(myMonth))
		}
       	this.periodYears = [];
       	this.yearHeaderAnchorIds = [];
		for(let myYear = DateTime.fromObject({year: this.start.getFullYear(), month: 1, day: 1}); 
			myYear.toMillis() <= DateTime.fromJSDate(this.end).toMillis();			 
			myYear = myYear.plus({years: 1})) {						
			this.periodYears.push(myYear);
			this.yearHeaderAnchorIds.push('Y_'+this.generateHeaderAnchorId(myYear));						
		}	
		/*
		if(!this.showDays) {
			this.dayPx =  (12 * (this.MONTH_WIDTH + 2)) -2 * this.periodYears.length;
		} else {
			this.dayPx = this.periodMonths.reduce<number>((px, myPeriodMonth) => px + (myPeriodMonth.daysInMonth * (this.DAY_WIDTH + 2)) -2, 0);			
		}
		console.log(this.dayPx);
		*/
	}
	
	get items(): Item<Event>[] {
		return this.localItems;
	}
	
	@Input({required: true})	
	set items(items: Item<Event>[]) {
		this.localItems = items;
		this.calcChartTime();		
	}
	
	get start(): Date {
		return this.localStart;
	}
	
	@Input({required: true})
	set start(start: Date) {
		this.localStart = DateTime.fromJSDate(start).setLocale(this.locale).setZone(Intl.DateTimeFormat().resolvedOptions().timeZone).toJSDate();
		this.calcChartTime();
	}
	
	get showDays(): boolean {
		return this.localShowDays;
	}
	
	@Input({required: true})
	set showDays(showDays: boolean) {
		this.localShowDays = showDays;
		this.calcChartTime();
	}
}
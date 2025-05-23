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
package ch.xxx.manager.usecase.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

@Service
public class NewsFeedService {
	private static final Logger LOGGER = LoggerFactory.getLogger(NewsFeedService.class);
	
	private final NewsFeedClient newsFeedClient;
	private volatile Optional<SyndFeed> seekingAlphaNewsFeedOptional = Optional.empty();
	private volatile Optional<SyndFeed> cnbcFinanceNewsFeedOptional = Optional.empty();
	
	public NewsFeedService(NewsFeedClient newsFeedClient) {
		this.newsFeedClient = newsFeedClient;
	}

	@Async
	public void updateSeekingAlphaNewsFeed() {
		var start = Instant.now();
		this.seekingAlphaNewsFeedOptional = Optional.ofNullable(this.newsFeedClient.importSeekingAlphaFeed());
		LOGGER.info("Seeking Alpha news imported in: {}ms", Instant.now().toEpochMilli() - start.toEpochMilli()); 
	}
	
	@Async
	public void updateCnbcFinanceNewsFeed() {
		var start = Instant.now();
		this.cnbcFinanceNewsFeedOptional = Optional.ofNullable(this.newsFeedClient.importCnbcFinanceNewsFeed());
		LOGGER.info("Cnbc news imported in: {}ms", Instant.now().toEpochMilli() - start.toEpochMilli());
	}
	
	public List<SyndEntry> getSeekingAlphaNewsFeed() {		
		return this.seekingAlphaNewsFeedOptional.stream().flatMap(myFeed -> myFeed.getEntries().stream()).map(myEntry -> {
			myEntry.getForeignMarkup().clear();
			return myEntry;
		}).toList();
	}
	
	public List<SyndEntry> getCnbcFinanceNewsFeed() {
		return this.cnbcFinanceNewsFeedOptional.stream().flatMap(myFeed -> myFeed.getEntries().stream()).map(myEntry -> {
			myEntry.getForeignMarkup().clear();
			return myEntry;
		}).toList();
	}
}

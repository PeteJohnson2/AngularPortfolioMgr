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
package ch.xxx.manager.adapter.messaging;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.xxx.manager.adapter.config.KafkaConfig;
import ch.xxx.manager.domain.model.dto.AppUserDto;
import ch.xxx.manager.domain.model.dto.RevokedTokenDto;

@Service
@Transactional
@Profile("kafka | prod-kafka")
public class KafkaConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

	@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), autoCreateTopics = "false", topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
	@KafkaListener(topics = KafkaConfig.NEW_USER_TOPIC)
	public void consumerForNewUserTopic(String message) throws JsonMappingException, JsonProcessingException {
		LOGGER.info("consumberForNewUserTopic [{}]", message);
		new ObjectMapper().readValue(message, AppUserDto.class);
	}

	@DltHandler
	public void dlt(String in, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		LOGGER.info(in + " from " + topic);
	}

	@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), autoCreateTopics = "false", topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
	@KafkaListener(topics = KafkaConfig.USER_LOGOUT_TOPIC)
	public void consumerForUserLogoutsTopic(String message) throws JsonMappingException, JsonProcessingException {
		LOGGER.info("consumerForUserLogoutsTopic [{}]", message);
		new ObjectMapper().readValue(message, RevokedTokenDto.class);
	}
}
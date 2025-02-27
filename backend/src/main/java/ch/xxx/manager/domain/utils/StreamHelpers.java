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
package ch.xxx.manager.domain.utils;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamHelpers {
	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {

		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	public static <T> Stream<T> toStream(Collection<T> collection) {
		return Optional.ofNullable(collection).stream().flatMap(myList -> myList.stream());
	}

	public static <T> Stream<T> toStream(T[] array) {
		return Optional.ofNullable(array).stream().flatMap(myArray -> List.of(array).stream());
	}

	public static <T> Stream<T> toStream(Iterable<T> iterable) {
		return Optional.ofNullable(iterable).stream()
				.flatMap(myIterable -> StreamSupport.stream(myIterable.spliterator(), false));
	}

	public static <T> Stream<T> toStream(T object) {
		return Optional.ofNullable(object).stream();
	}

	public static <T> Stream<T> unboxOptionals(Stream<Optional<T>> optSteam) {
		return optSteam.flatMap(Optional::stream);
	}

	public static <T> Stream<T> optionalStream(Optional<T> opt) {
		return opt.stream();
	}
	
	public static <T> Stream<T> convert(Enumeration<T> enumeration) {
	    EnumerationSpliterator<T> spliterator 
	      = new EnumerationSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED, enumeration);
	    Stream<T> stream = StreamSupport.stream(spliterator, false);

	    return stream;
	}
}

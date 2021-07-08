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
package ch.xxx.manager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ch.xxx.manager.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
		@Query("select au from AppUser au where au.userName = :username")
		Optional<AppUser> findByUsername(String username);
		@Query("select au from AppUser au where au.uuid = :uuid")
		Optional<AppUser> findByUuid(String uuid);
}

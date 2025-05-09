package com.siemens.internship;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternshipApplicationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void createItem_minimalValidFields() {
		Item item = new Item(null, "A", "", "OPEN", "a@a.com");
		ResponseEntity<Item> response = restTemplate.postForEntity("/api/items", item, Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getName()).isEqualTo("A");
	}

	@Test
	void createItem_longNameAndStatus() {
		Item item = new Item(null, "VeryLongItemNameThatStillWorks", "Detailed description", "IN_PROGRESS", "long@example.org");
		ResponseEntity<Item> response = restTemplate.postForEntity("/api/items", item, Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getStatus()).isEqualTo("IN_PROGRESS");
	}

	@Test
	void createItem_blankStatus_shouldFail() {
		Item item = new Item(null, "Item", "desc", "", "test@mail.com");
		ResponseEntity<String> response = restTemplate.postForEntity("/api/items", item, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createItem_nullName_shouldFail() {
		Item item = new Item(null, null, "desc", "NEW", "null@fail.com");
		ResponseEntity<String> response = restTemplate.postForEntity("/api/items", item, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createItem_emailWithCapitalLetters() {
		Item item = new Item(null, "CapitalEmail", "desc", "PENDING", "Test@Domain.COM");
		ResponseEntity<Item> response = restTemplate.postForEntity("/api/items", item, Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getEmail()).isEqualTo("Test@Domain.COM");
	}
	
	@Test
	void getAllItems_afterMultipleInserts_returnsAll() {
		Item item1 = new Item(null, "Item A", "Desc A", "PENDING", "a@example.com");
		Item item2 = new Item(null, "Item B", "Desc B", "DONE", "b@example.com");

		restTemplate.postForEntity("/api/items", item1, Item.class);
		restTemplate.postForEntity("/api/items", item2, Item.class);

		ResponseEntity<Item[]> response = restTemplate.getForEntity("/api/items", Item[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().length).isGreaterThanOrEqualTo(2);
	}

	@Test
	void getItemById_negativeId_returnsNotFound() {
		ResponseEntity<Item> response = restTemplate.getForEntity("/api/items/-1", Item.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getItemById_repeatedAccess_returnsConsistentResult() {
		Item item = new Item(null, "Cached Item", "Desc", "STORED", "cached@example.com");
		ResponseEntity<Item> postResponse = restTemplate.postForEntity("/api/items", item, Item.class);

		Long id = postResponse.getBody().getId();
		for (int i = 0; i < 3; i++) {
			ResponseEntity<Item> getResponse = restTemplate.getForEntity("/api/items/" + id, Item.class);
			assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(getResponse.getBody().getId()).isEqualTo(id);
		}
	}

	@Test
	void getItemById_leadingZeros_returnsNotFound() {
		ResponseEntity<Item> response = restTemplate.getForEntity("/api/items/000123", Item.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getItemById_immediatelyAfterCreation_returnsItem() {
		Item item = new Item(null, "Fresh Item", "Just added", "NEW", "new@item.com");
		ResponseEntity<Item> createResponse = restTemplate.postForEntity("/api/items", item, Item.class);

		Long id = createResponse.getBody().getId();
		ResponseEntity<Item> getResponse = restTemplate.getForEntity("/api/items/" + id, Item.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getResponse.getBody().getEmail()).isEqualTo("new@item.com");
	}

	@Test
	void updateItem_allFieldsUpdated_returnsUpdatedItem() {
		Item original = new Item(null, "ToUpdate", "Original", "PENDING", "up1@domain.com");
		ResponseEntity<Item> postResponse = restTemplate.postForEntity("/api/items", original, Item.class);
		Long id = postResponse.getBody().getId();

		Item updated = new Item(null, "UpdatedName", "UpdatedDesc", "DONE", "updated@domain.com");
		HttpEntity<Item> request = new HttpEntity<>(updated);

		ResponseEntity<Item> response = restTemplate.exchange("/api/items/" + id, HttpMethod.PUT, request, Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getName()).isEqualTo("UpdatedName");
		assertThat(response.getBody().getStatus()).isEqualTo("DONE");
	}

	@Test
	void updateItem_nonExistentId_returnsNotFound() {
		Item item = new Item(null, "Ghost", "None", "INVALID", "ghost@ghost.com");
		HttpEntity<Item> request = new HttpEntity<>(item);

		ResponseEntity<Item> response = restTemplate.exchange("/api/items/999999", HttpMethod.PUT, request, Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void updateItem_withEmptyName_returnsBadRequest() {
		Item original = new Item(null, "NameToClear", "Desc", "ACTIVE", "clear@domain.com");
		ResponseEntity<Item> post = restTemplate.postForEntity("/api/items", original, Item.class);
		Long id = post.getBody().getId();

		Item updated = new Item(null, "", "NoName", "VOID", "still@valid.com");
		HttpEntity<Item> req = new HttpEntity<>(updated);
		ResponseEntity<String> put = restTemplate.exchange("/api/items/" + id, HttpMethod.PUT, req, String.class);

		assertThat(put.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}


	@Test
	void updateItem_onlyEmailUpdated_returnsModifiedEmail() {
		Item original = new Item(null, "EmailOnly", "Ignore", "HOLD", "old@email.com");
		ResponseEntity<Item> post = restTemplate.postForEntity("/api/items", original, Item.class);
		Long id = post.getBody().getId();

		Item partialUpdate = new Item(null, "EmailOnly", "Ignore", "HOLD", "new@email.com");
		HttpEntity<Item> req = new HttpEntity<>(partialUpdate);
		ResponseEntity<Item> response = restTemplate.exchange("/api/items/" + id, HttpMethod.PUT, req, Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getEmail()).isEqualTo("new@email.com");
	}

	@Test
	void updateItem_noChanges_returnsSameData() {
		Item item = new Item(null, "NoChange", "SameDesc", "WAIT", "same@domain.com");
		ResponseEntity<Item> created = restTemplate.postForEntity("/api/items", item, Item.class);
		Long id = created.getBody().getId();

		HttpEntity<Item> request = new HttpEntity<>(item);
		ResponseEntity<Item> updated = restTemplate.exchange("/api/items/" + id, HttpMethod.PUT, request, Item.class);

		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().getName()).isEqualTo("NoChange");
	}

	@Test
	void deleteItem_existing_deletesSuccessfully() {
		Item toDelete = new Item(null, "ToDelete", "Temp", "EXPIRED", "delete@now.com");
		ResponseEntity<Item> post = restTemplate.postForEntity("/api/items", toDelete, Item.class);
		Long id = post.getBody().getId();

		ResponseEntity<Void> delete = restTemplate.exchange("/api/items/" + id, HttpMethod.DELETE, null, Void.class);
		assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<Item> getAfterDelete = restTemplate.getForEntity("/api/items/" + id, Item.class);
		assertThat(getAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteItem_twice_secondTimeNotFound() {
		Item item = new Item(null, "SingleDelete", "OnlyOnce", "REMOVED", "once@delete.com");
		ResponseEntity<Item> post = restTemplate.postForEntity("/api/items", item, Item.class);
		Long id = post.getBody().getId();

		ResponseEntity<Void> firstDelete = restTemplate.exchange("/api/items/" + id, HttpMethod.DELETE, null, Void.class);
		assertThat(firstDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<Void> secondDelete = restTemplate.exchange("/api/items/" + id, HttpMethod.DELETE, null, Void.class);
		assertThat(secondDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteItem_nonexistent_returnsNotFound() {
		ResponseEntity<Void> delete = restTemplate.exchange("/api/items/123456789", HttpMethod.DELETE, null, Void.class);
		assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void processItems_whenItemsExist_allAreProcessed() {
		Item item1 = new Item(null, "Batch1", "Pending", "INIT", "batch1@test.com");
		Item item2 = new Item(null, "Batch2", "Pending", "INIT", "batch2@test.com");
		restTemplate.postForEntity("/api/items", item1, Item.class);
		restTemplate.postForEntity("/api/items", item2, Item.class);

		ResponseEntity<Item[]> response = restTemplate.getForEntity("/api/items/process", Item[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		for (Item processed : response.getBody()) {
			assertThat(processed.getStatus()).isEqualTo("PROCESSED");
		}
	}

	@Test
	void processItems_whenNoItems_returnsEmptyArray() {
		ResponseEntity<Item[]> initial = restTemplate.getForEntity("/api/items", Item[].class);
		assertThat(initial.getBody()).isNotNull();
		for (Item i : initial.getBody()) {
			restTemplate.delete("/api/items/" + i.getId());
		}

		ResponseEntity<Item[]> response = restTemplate.getForEntity("/api/items/process", Item[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().length).isEqualTo(0);
	}

	@Test
	void processItems_whenSomeAlreadyProcessed_keepsProcessedStatus() {
		Item alreadyProcessed = new Item(null, "DoneItem", "Was processed", "PROCESSED", "done@test.com");
		Item toProcess = new Item(null, "ToProcess", "Needs work", "NEW", "do@test.com");

		restTemplate.postForEntity("/api/items", alreadyProcessed, Item.class);
		restTemplate.postForEntity("/api/items", toProcess, Item.class);

		ResponseEntity<Item[]> response = restTemplate.getForEntity("/api/items/process", Item[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();

		boolean foundProcessed = false;
		boolean foundUpdated = false;
		for (Item item : response.getBody()) {
			if (item.getName().equals("DoneItem")) {
				assertThat(item.getStatus()).isEqualTo("PROCESSED");
				foundProcessed = true;
			} else if (item.getName().equals("ToProcess")) {
				assertThat(item.getStatus()).isEqualTo("PROCESSED");
				foundUpdated = true;
			}
		}
		assertThat(foundProcessed && foundUpdated).isTrue();
	}
	
	@Test
	void processItems_whenItemDeletedMidProcess_returnsOkWithPartialResults() {
		Item item = new Item(null, "Ghost", "Will vanish", "ACTIVE", "ghost@test.com");
		ResponseEntity<Item> post = restTemplate.postForEntity("/api/items", item, Item.class);
		assertThat(post.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		Long id = post.getBody().getId();
		restTemplate.delete("/api/items/" + id);

		ResponseEntity<Item[]> response = restTemplate.getForEntity("/api/items/process", Item[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull(); 
	}
	
	@Test
	void processItems_shouldUpdateStatusesAndReturnAll() {

	    Item i1 = new Item(null, "Alpha", "Desc", "INIT", "a@a.com");
	    Item i2 = new Item(null, "Beta", "Desc", "INIT", "b@b.com");

	    ResponseEntity<Item> r1 = restTemplate.postForEntity("/api/items", i1, Item.class);
	    ResponseEntity<Item> r2 = restTemplate.postForEntity("/api/items", i2, Item.class);

	    ResponseEntity<Item[]> response = restTemplate.getForEntity("/api/items/process", Item[].class);


	    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	    Item[] processed = response.getBody();
	    assertThat(processed).isNotNull();
	    assertThat(processed.length).isGreaterThanOrEqualTo(2);

	    for (Item item : processed) {
	        assertThat(item.getStatus()).isEqualTo("PROCESSED");
	    }

	    ResponseEntity<Item> updated1 = restTemplate.getForEntity("/api/items/" + r1.getBody().getId(), Item.class);
	    assertThat(updated1.getBody().getStatus()).isEqualTo("PROCESSED");
	}
}
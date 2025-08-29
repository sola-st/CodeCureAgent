```java
/**
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kaczmarzyk;

import net.kaczmarzyk.spring.data.jpa.Customer;
import net.kaczmarzyk.spring.data.jpa.CustomerRepository;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.In;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.domain.LikeIgnoreCase;
import net.kaczmarzyk.spring.data.jpa.web.annotation.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author Tomasz Kaczmarzyk
 */
public class JoinE2eTest extends E2eTestBase {

	private static final String JOIN_CUSTOMERS_PATH = "/join/customers";
	private static final String MULTI_JOIN_CUSTOMERS_PATH = "/multi-join/customers";
	private static final String JOIN_PAGEABLE_CUSTOMERS_PATH = "/join-pageable/customers";
	private static final String PARAM_FIRST_NAME = "firstName";
	private static final String PARAM_ORDER = "order";
	private static final String PARAM_ORDER1 = "order1";
	private static final String PARAM_ORDER2 = "order2";
	private static final String PARAM_ORDER_IN = "orderIn";
	private static final String PARAM_BADGE = "badge";
	private static final String ACCEPT_JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String SORT_ID = "id";
	private static final String PAGE = "page";
	private static final String SIZE = "size";


	@Controller
	public static class TestController {

		@Autowired
		CustomerRepository customerRepo;

		@RequestMapping(value = JOIN_CUSTOMERS_PATH)
		@ResponseBody
		public Object findByNameAndOrders(

				@Join(path = "orders", alias = "o")
				@And({
						@Spec(path = "firstName", spec = Equal.class),
						@Spec(path = "o.itemName", params = PARAM_ORDER, spec = LikeIgnoreCase.class)
				}) Specification<Customer> spec) {

			return customerRepo.findAll(spec, Sort.by(SORT_ID));
		}

		@RequestMapping(value = JOIN_CUSTOMERS_PATH, params = { PARAM_ORDER1, PARAM_ORDER2 })
		@ResponseBody
		public Object findByOrder2Options(

				@Join(path = "orders", alias = "o")
				@Or({
						@Spec(path = "o.itemName", params = PARAM_ORDER1, spec = Like.class),
						@Spec(path = "o.itemName", params = PARAM_ORDER2, spec = Equal.class)
				}) Specification<Customer> spec) {

			return customerRepo.findAll(spec, Sort.by(SORT_ID));
		}

		@Join(path = "orders", alias = "o")
		@Spec(path = "o.itemName", params = PARAM_ORDER_IN, spec = In.class)
		public interface OrderInSpecification extends Specification<Customer> {
		}

		@RequestMapping(value = JOIN_CUSTOMERS_PATH, params = { PARAM_ORDER_IN })
		@ResponseBody
		public Object findByOrderIn(OrderInSpecification spec) {
			return customerRepo.findAll(spec, Sort.by(SORT_ID));
		}

		@RequestMapping(value = MULTI_JOIN_CUSTOMERS_PATH, params = { PARAM_ORDER, PARAM_BADGE })
		@ResponseBody
		public Object findByOrderAndOrders2(

				@Joins({
						@Join(path = "orders", alias = "o"),
						@Join(path = "badges", alias = "b")
				})
				@Or({
						@Spec(path = "o.itemName", params = PARAM_ORDER, spec = Like.class),
						@Spec(path = "b.badgeType", params = PARAM_BADGE, spec = Equal.class)
				}) Specification<Customer> spec) {

			return customerRepo.findAll(spec, Sort.by(SORT_ID));
		}

		@RequestMapping(value = JOIN_PAGEABLE_CUSTOMERS_PATH)
		@ResponseBody
		public Object findByNameAndOrdersWithPagination(

				@Join(path = "orders", alias = "o")
				@And({
						@Spec(path = "firstName", spec = Equal.class),
						@Spec(path = "o.itemName", params = PARAM_ORDER, spec = LikeIgnoreCase.class)
				}) Specification<Customer> spec,
				Pageable pageable) {

			return customerRepo.findAll(spec, pageable);
		}
	}

	@Test
	public void findsByOrdersAndName() throws Exception {
		mockMvc.perform(get(JOIN_CUSTOMERS_PATH)
				.param(PARAM_FIRST_NAME, "Homer")
				.param(PARAM_ORDER, "Duff Beer")
				.accept(ACCEPT_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].firstName").value("Homer"))
			.andExpect(jsonPath("$[1]").doesNotExist());
	}

	@Test
	public void findsByOrders() throws Exception {
		mockMvc.perform(get(JOIN_CUSTOMERS_PATH)
				.param(PARAM_ORDER, "Duff Beer")
				.accept(ACCEPT_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].firstName").value("Homer"))
			.andExpect(jsonPath("$[1].firstName").value("Moe"))
			.andExpect(jsonPath("$[2]").doesNotExist());
	}

	@Test
	public void createsDistinctQueryByDefault() throws Exception {
		mockMvc.perform(get(JOIN_CUSTOMERS_PATH)
				.param(PARAM_ORDER1, "Beer")
				.param(PARAM_ORDER2, "Donuts")
				.accept(ACCEPT_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].firstName").value("Homer"))
			.andExpect(jsonPath("$[1].firstName").value("Moe"))
			.andExpect(jsonPath("$[2]").doesNotExist());
	}

	@Test
	public void resolvesJoinProperlyFromAnnotatedCustomInterface() throws Exception {
		mockMvc.perform(get(JOIN_CUSTOMERS_PATH)
				.param(PARAM_ORDER_IN, "Pizza")
				.accept(ACCEPT_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].firstName").value("Homer"))
			.andExpect(jsonPath("$[1]").doesNotExist());
	}

	@Test
	public void filtersByAttributesOfMultipleJoins() throws Exception {
		mockMvc.perform(get(MULTI_JOIN_CUSTOMERS_PATH)
				.param(PARAM_ORDER, "Pizza")
				.param(PARAM_BADGE, "Troll Face")
				.accept(ACCEPT_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].firstName").value("Homer"))
			.andExpect(jsonPath("$[1].firstName").value("Moe"))
			.andExpect(jsonPath("$[2]").doesNotExist());
	}

	@Test
	public void findsByOrdersWithPagination() throws Exception {
		mockMvc.perform(get(JOIN_PAGEABLE_CUSTOMERS_PATH)
				.param(PARAM_ORDER, "Duff Beer")
				.param(PAGE, "0")
				.param(SIZE, "1")
				.param("sort", SORT_ID)
				.accept(ACCEPT_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].firstName").value("Homer"))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.size").value(1));
	}

}

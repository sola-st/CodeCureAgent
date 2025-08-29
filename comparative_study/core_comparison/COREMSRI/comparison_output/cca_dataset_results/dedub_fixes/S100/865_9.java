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
package net.kaczmarzyk.spring.data.jpa.web;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tomasz Kaczmarzyk
 * @author Jakub Radlica
 */
@RunWith(Parameterized.class)
public class WebRequestProcessingContextPathVariableResolverTest {
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Parameters(name = "TestController: {0}")
	public static Collection testController() {
		return Arrays.asList(
				TestControllerWithClassLevelRequestMappingWithValue.class,
				TestControllerWithClassLevelRequestMappingWithValueAndPathVarWithRegexp.class,
				TestControllerWithClassLevelRequestMappingWithPath.class,
				TestControllerWithClassLevelRequestMappingWithPathAndPathVarWithRegexp.class
		);
	}
	
	private Class<?> testController;
	
	public WebRequestProcessingContextPathVariableResolverTest(Class<?> testController) {
		this.testController = testController;
	}
	
	@Test
	public void resolvesPathVariableFromClassLevelRequestMapingAndMethodLevelRequestMapping_empty() {
		MockWebRequest req = new MockWebRequest("/customers/888");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableRequestMappingEmpty", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
	}
	
	@Test
	public void resolvesPathVariableFromClassLevelRequestMapingAndMethodLevelGetMapping_empty() {
		MockWebRequest req = new MockWebRequest("/customers/888");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableGetMappingEmpty", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
	}
	
	@Test
	public void resolvesPathVariableFromClassLevelRequestMappingAndMethodLevelRequestMapping_value() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableRequestMappingValue", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@Test
	public void resolvesPathVariableWithRegexpFromClassLevelRequestMappingAndMethodLevelRequestMapping_value() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableWithRegexpRequestMappingValue", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@Test
	public void resolvesPathVariableFromClassLevelRequestMappingAndMethodLevelRequestMapping_path() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableRequestMappingPath", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@Test
	public void resolvesPathVariableWithRegexpFromClassLevelRequestMappingAndMethodLevelRequestMapping_path() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableWithRegexpRequestMappingPath", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@Test
	public void resolvesPathVariableFromClassLevelRequestMappingAndMethodLevelGetMapping_value() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableGetMappingValue", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@Test
	public void resolvesPathVariableWithRegexpFromClassLevelRequestMappingAndMethodLevelGetMapping_value() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableWithRegexpGetMappingValue", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@Test
	public void resolvesPathVariableFromClassLevelRequestMappingAndMethodLevelGetMapping_path() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableGetMappingPath", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@Test
	public void resolvesPathVariableWithRegexpFromClassLevelRequestMappingAndMethodLevelGetMapping_path() {
		MockWebRequest req = new MockWebRequest("/customers/888/orders/99");
		WebRequestProcessingContext context = new WebRequestProcessingContext(
				testMethodParameter("testMethodUsingPathVariableWithRegexpGetMappingPath", testController), req);
		
		assertThat(context.getPathVariableValue("customerId")).isEqualTo("888");
		assertThat(context.getPathVariableValue("orderId")).isEqualTo("99");
	}
	
	@RequestMapping("/customers/{customerId}")
	public static class TestControllerWithClassLevelRequestMappingWithValue {
		
		@RequestMapping
		public void testMethodUsingPathVariableRequestMappingEmpty(Specification<Object> spec) {
		}
		
		@GetMapping
		public void testMethodUsingPathVariableGetMappingEmpty(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingValue(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId:.*}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingValue(Specification<Object> spec) {
		}
	}
	
	@RequestMapping("/customers/{customerId:.*}")
	public static class TestControllerWithClassLevelRequestMappingWithValueAndPathVarWithRegexp {
		
		@RequestMapping
		public void testMethodUsingPathVariableRequestMappingEmpty(Specification<Object> spec) {
		}
		
		@GetMapping
		public void testMethodUsingPathVariableGetMappingEmpty(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingValue(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId:.*}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingValue(Specification<Object> spec) {
		}
	}
	
	@RequestMapping(path = "/customers/{customerId}")
	public static class TestControllerWithClassLevelRequestMappingWithPath {
		
		@RequestMapping
		public void testMethodUsingPathVariableRequestMappingEmpty(Specification<Object> spec) {
		}
		
		@GetMapping
		public void testMethodUsingPathVariableGetMappingEmpty(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingValue(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId:.*}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingValue(Specification<Object> spec) {
		}
	}
	
	@RequestMapping(path = "/customers/{customerId:[0-9]+}")
	public static class TestControllerWithClassLevelRequestMappingWithPathAndPathVarWithRegexp {
		
		@RequestMapping
		public void testMethodUsingPathVariableRequestMappingEmpty(Specification<Object> spec) {
		}
		
		@GetMapping
		public void testMethodUsingPathVariableGetMappingEmpty(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingPath(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableRequestMappingValue(Specification<Object> spec) {
		}
		
		@RequestMapping(value = "/orders/{orderId:.*}")
		public void testMethodUsingPathVariableWithRegexpRequestMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(path = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingPath(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId}")
		public void testMethodUsingPathVariableGetMappingValue(Specification<Object> spec) {
		}
		
		@GetMapping(value = "/orders/{orderId:[0-9]+}")
		public void testMethodUsingPathVariableWithRegexpGetMappingValue(Specification<Object> spec) {
		}
	}
	
	private MethodParameter testMethodParameter(String methodName, Class<?> controllerClass) {
		return MethodParameter.forExecutable(testMethod(methodName, controllerClass, Specification.class), 0);
	}
	
	private Executable testMethod(String methodName, Class<?> controllerClass, Class<?> specClass) {
		try {
			return controllerClass.getMethod(methodName, specClass);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

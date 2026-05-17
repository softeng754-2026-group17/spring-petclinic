/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vet;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Handles HTTP requests for displaying {@link Vet} information, supporting both a
 * paginated HTML list view and a JSON resource endpoint.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class VetController {

	private final VetRepository vetRepository;

	/**
	 * Creates a new {@code VetController} backed by the given vet repository.
	 * @param vetRepository the repository used to retrieve {@link Vet} data
	 */
	public VetController(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	/**
	 * Displays a paginated HTML list of all veterinarians.
	 * @param page the 1-based page number to display; defaults to {@code 1}
	 * @param model the model to populate with pagination and vet list attributes
	 * @return the logical view name for the vet list page
	 */
	@GetMapping("/vets.html")
	public String showVetList(@RequestParam(defaultValue = "1") int page, Model model) {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for Object-Xml mapping
		Vets vets = new Vets();
		Page<Vet> paginated = findPaginated(page);
		vets.getVetList().addAll(paginated.toList());
		return addPaginationModel(page, paginated, model);
	}

	/**
	 * Adds pagination-related attributes to the model for the vet list view.
	 * @param page the current 1-based page number
	 * @param paginated the paginated result containing vets for the current page
	 * @param model the model to populate
	 * @return the logical view name for the vet list page
	 */
	private String addPaginationModel(int page, Page<Vet> paginated, Model model) {
		List<Vet> listVets = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listVets", listVets);
		return "vets/vetList";
	}

	/**
	 * Retrieves a page of vets for the given 1-based page number, using a fixed page
	 * size of 5.
	 * @param page the 1-based page number to retrieve
	 * @return a {@link Page} of {@link Vet}s for the requested page
	 */
	private Page<Vet> findPaginated(int page) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return vetRepository.findAll(pageable);
	}

	/**
	 * Returns all veterinarians as a JSON response body.
	 * @return a {@link Vets} wrapper containing all {@link Vet} instances
	 */
	@GetMapping({ "/vets" })
	public @ResponseBody Vets showResourcesVetList() {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for JSon/Object mapping
		Vets vets = new Vets();
		vets.getVetList().addAll(this.vetRepository.findAll());
		return vets;
	}

}

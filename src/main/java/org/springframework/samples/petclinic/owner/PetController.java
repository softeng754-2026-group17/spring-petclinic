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
package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles HTTP requests for managing {@link Pet}s belonging to a specific {@link Owner},
 * including creating and updating pets under the {@code /owners/{ownerId}} base path.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private static final String OWNER_NOT_FOUND_MSG = "Owner not found with id: ";

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	/**
	 * Creates a new {@code PetController} backed by the given repositories.
	 * @param owners the repository used to load and persist {@link Owner} instances
	 * @param types the repository used to retrieve all available {@link PetType}s
	 */
	public PetController(OwnerRepository owners, PetTypeRepository types) {
		this.owners = owners;
		this.types = types;
	}

	/**
	 * Populates the model with all available pet types for use in pet creation and update
	 * forms.
	 * @return a collection of all known {@link PetType}s
	 */
	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.types.findPetTypes();
	}

	/**
	 * Loads the {@link Owner} with the given identifier for use in model binding.
	 * @param ownerId the identifier of the owner from the URL path
	 * @return the matching {@link Owner}
	 * @throws IllegalArgumentException if no owner with the given identifier exists
	 */
	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		return optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				OWNER_NOT_FOUND_MSG + ownerId + ". Please ensure the ID is correct "));
	}

	/**
	 * Loads an existing {@link Pet} when {@code petId} is present in the URL, or returns
	 * a new empty {@link Pet} for creation forms.
	 * @param ownerId the identifier of the owning {@link Owner}
	 * @param petId the pet identifier from the URL path, or {@code null} for new pet
	 * forms
	 * @return the existing {@link Pet} or a new empty instance
	 * @throws IllegalArgumentException if the owner does not exist
	 */
	@ModelAttribute("pet")
	public Pet findPet(@PathVariable("ownerId") int ownerId,
			@PathVariable(name = "petId", required = false) Integer petId) {

		if (petId == null) {
			return new Pet();
		}

		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				OWNER_NOT_FOUND_MSG + ownerId + ". Please ensure the ID is correct "));
		return owner.getPet(petId);
	}

	/**
	 * Prevents clients from directly setting entity identifiers on {@link Owner} objects
	 * through form binding.
	 * @param dataBinder the binder to configure
	 */
	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Configures the data binder for {@link Pet} objects, registering
	 * {@link PetValidator} for validation and preventing clients from setting entity
	 * identifiers.
	 * @param dataBinder the binder to configure
	 */
	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Renders the pet creation form, adding a new unsaved {@link Pet} to the owner.
	 * @param owner the owner to whom the new pet will belong
	 * @param model the model map shared with the view
	 * @return the logical view name for the create/update pet form
	 */
	@GetMapping("/pets/new")
	public String initCreationForm(Owner owner) {
		Pet pet = new Pet();
		owner.addPet(pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Processes the pet creation form. Validates that the pet name is unique for the
	 * owner and that the birth date is not in the future. Saves the pet on success or
	 * returns the form view with errors.
	 * @param owner the owner to whom the pet will be added
	 * @param pet the new pet populated from the submitted form
	 * @param result binding result containing any validation errors
	 * @param redirectAttributes flash attributes for passing a success message
	 * @return a redirect to the owner detail page on success, or the form view on error
	 */
	@PostMapping("/pets/new")
	public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {

		if (StringUtils.hasText(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
			result.rejectValue("name", "duplicate", "already exists");
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		owner.addPet(pet);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Pet has been Added");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Renders the pet update form, pre-populated with the current pet data.
	 * @return the logical view name for the create/update pet form
	 */
	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm() {
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Processes the pet update form. Validates that the updated name does not duplicate
	 * another pet on the same owner and that the birth date is not in the future. Saves
	 * the changes on success or returns the form view with errors.
	 * @param owner the owner of the pet being updated
	 * @param pet the pet with updated values from the submitted form
	 * @param result binding result containing any validation errors
	 * @param redirectAttributes flash attributes for passing a success message
	 * @return a redirect to the owner detail page on success, or the form view on error
	 */
	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {

		String petName = pet.getName();

		// checking if the pet name already exists for the owner
		if (StringUtils.hasText(petName)) {
			Pet existingPet = owner.getPet(petName, false);
			if (existingPet != null && !Objects.equals(existingPet.getId(), pet.getId())) {
				result.rejectValue("name", "duplicate", "already exists");
			}
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		updatePetDetails(owner, pet);
		redirectAttributes.addFlashAttribute("message", "Pet details has been edited");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Updates the pet details if it exists or adds a new pet to the owner.
	 * @param owner The owner of the pet
	 * @param pet The pet with updated details
	 */
	private void updatePetDetails(Owner owner, Pet pet) {
		Integer id = pet.getId();
		Assert.state(id != null, "'pet.getId()' must not be null");
		Pet existingPet = owner.getPet(id);
		if (existingPet != null) {
			// Update existing pet's properties
			existingPet.setName(pet.getName());
			existingPet.setBirthDate(pet.getBirthDate());
			existingPet.setType(pet.getType());
		}
		else {
			owner.addPet(pet);
		}
		this.owners.save(owner);
	}

}

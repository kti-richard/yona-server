/*******************************************************************************
 * Copyright (c) 2015, 2016 Stichting Yona Foundation This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *******************************************************************************/
package nu.yona.server.goals.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.annotation.JsonView;

import nu.yona.server.goals.rest.ActivityCategoryController.ActivityCategoryResource;
import nu.yona.server.goals.service.ActivityCategoryDto;
import nu.yona.server.goals.service.ActivityCategoryService;

@Controller
@ExposesResourceFor(ActivityCategoryResource.class)
@RequestMapping(value = "/activityCategories", produces = { MediaType.APPLICATION_JSON_VALUE })
public class ActivityCategoryController
{
	@Autowired
	private ActivityCategoryService activityCategoryService;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(ActivityCategoryDto.AdminView.class)
	public HttpEntity<ActivityCategoryResource> getActivityCategory(@PathVariable UUID id)
	{
		return createOkResponse(activityCategoryService.getActivityCategory(id));
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(ActivityCategoryDto.AdminView.class)
	public HttpEntity<Resources<ActivityCategoryResource>> getAllActivityCategories()
	{
		return createOkResponse(activityCategoryService.getAllActivityCategories(), getAllActivityCategoriesLinkBuilder());
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	@JsonView(ActivityCategoryDto.AdminView.class)
	public HttpEntity<ActivityCategoryResource> addActivityCategory(@RequestBody ActivityCategoryDto activityCategory)
	{
		activityCategory.setId(UUID.randomUUID());
		return createOkResponse(activityCategoryService.addActivityCategory(activityCategory));
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	@ResponseBody
	@JsonView(ActivityCategoryDto.AdminView.class)
	public HttpEntity<ActivityCategoryResource> updateActivityCategory(@PathVariable UUID id,
			@RequestBody ActivityCategoryDto activityCategory)
	{
		return createOkResponse(activityCategoryService.updateActivityCategory(id, activityCategory));
	}

	@RequestMapping(value = "/", method = RequestMethod.PUT)
	@ResponseBody
	@JsonView(ActivityCategoryDto.AdminView.class)
	public HttpEntity<Resources<ActivityCategoryResource>> updateActivityCategorySet(
			@RequestBody Set<ActivityCategoryDto> activityCategorySet)
	{
		activityCategoryService.updateActivityCategorySet(activityCategorySet);
		return createOkResponse(activityCategoryService.getAllActivityCategories(), getAllActivityCategoriesLinkBuilder());
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteActivityCategory(@PathVariable UUID id)
	{
		activityCategoryService.deleteActivityCategory(id);
	}

	static ControllerLinkBuilder getAllActivityCategoriesLinkBuilder()
	{
		ActivityCategoryController methodOn = methodOn(ActivityCategoryController.class);
		return linkTo(methodOn.getAllActivityCategories());
	}

	private HttpEntity<ActivityCategoryResource> createOkResponse(ActivityCategoryDto activityCategory)
	{
		return createResponse(activityCategory, HttpStatus.OK);
	}

	private HttpEntity<ActivityCategoryResource> createResponse(ActivityCategoryDto activityCategory, HttpStatus status)
	{
		return new ResponseEntity<>(new ActivityCategoryResourceAssembler().toResource(activityCategory), status);
	}

	private HttpEntity<Resources<ActivityCategoryResource>> createOkResponse(Set<ActivityCategoryDto> activityCategories,
			ControllerLinkBuilder controllerMethodLinkBuilder)
	{
		return new ResponseEntity<>(
				wrapActivityCategoriesAsResourceList(activityCategories, controllerMethodLinkBuilder), HttpStatus.OK);
	}

	private Resources<ActivityCategoryResource> wrapActivityCategoriesAsResourceList(Set<ActivityCategoryDto> activityCategories,
			ControllerLinkBuilder controllerMethodLinkBuilder)
	{
		return new Resources<>(new ActivityCategoryResourceAssembler().toResources(activityCategories),
				controllerMethodLinkBuilder.withSelfRel());
	}

	public static class ActivityCategoryResource extends Resource<ActivityCategoryDto>
	{
		public ActivityCategoryResource(ActivityCategoryDto activityCategory)
		{
			super(activityCategory);
		}
	}

	private static class ActivityCategoryResourceAssembler
			extends ResourceAssemblerSupport<ActivityCategoryDto, ActivityCategoryResource>
	{
		public ActivityCategoryResourceAssembler()
		{
			super(ActivityCategoryController.class, ActivityCategoryResource.class);
		}

		@Override
		public ActivityCategoryResource toResource(ActivityCategoryDto activityCategory)
		{
			return super.createResourceWithId(activityCategory.getId(), activityCategory);
		}

		@Override
		protected ActivityCategoryResource instantiateResource(ActivityCategoryDto activityCategory)
		{
			return new ActivityCategoryResource(activityCategory);
		}
	}
}

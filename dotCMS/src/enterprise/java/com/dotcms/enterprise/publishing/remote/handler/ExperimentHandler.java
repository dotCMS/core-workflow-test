/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipFieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContentTypeBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ExperimentBundler;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.publisher.pusher.wrapper.ContentTypeWrapper;
import com.dotcms.publisher.pusher.wrapper.ExperimentWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.workflow.helper.SystemActionMappingsHandlerMerger;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Experiments-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link Experiment} data
 * files.
 *
 */
public class ExperimentHandler implements IHandler {

	private final PublisherConfig config;
	private final ExperimentsAPI experimentsAPI;
	public ExperimentHandler(final PublisherConfig config) {

		this.config = config;
		this.experimentsAPI = APILocator.getExperimentsAPI();
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@WrapInTransaction
	@Override
	public void handle(final File bundleFolder) throws Exception {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
        }

		final Collection<File> experiments = FileUtil.listFilesRecursively
				(bundleFolder, new ExperimentBundler().getFileFilter());

        handleExperiments(experiments);
	}

	private void handleExperiments(final Collection<File> experiments) throws DotPublishingException {
		File workingOn = null;
        Experiment experiment = null;
		try {
	        //Handle folders
	        for(final File experimentFile: experiments) {
				workingOn = experimentFile;
	        	if(experimentFile.isDirectory()) {

	        		continue;
				}

	        	final ExperimentWrapper experimentWrapper = BundlerUtil
						.jsonToObject(experimentFile, ExperimentWrapper.class);

				experiment = experimentWrapper.getExperiment();

	        	Optional<Experiment> localExperiment = experimentsAPI.find(experiment.id()
						.orElseThrow(), APILocator.systemUser());

	        	if(experimentWrapper.getOperation().equals(Operation.UNPUBLISH)) {
	        		// delete operation
	        	    if(localExperiment.isPresent()) {

						if(localExperiment.get().status()==Status.RUNNING ||
							localExperiment.get().status()==Status.SCHEDULED) {
							experimentsAPI.cancel(localExperiment.get().id().orElseThrow()
									, APILocator.systemUser());
						}

						experimentsAPI.delete(localExperiment.orElseThrow().id().orElseThrow(),
								APILocator.systemUser());
					}
	        	} else {
	        		// save or update Experiment
					if(experiment.status()== Status.RUNNING || experiment.status()== Status.SCHEDULED) {
						Experiment asDraft = Experiment.builder().from(experiment)
								.status(Status.DRAFT).build();

						final Optional<Scheduling> schedulingOptional = asDraft.scheduling();

						if(experiment.status()==Status.RUNNING) {
							asDraft = asDraft.withScheduling(Optional.empty());
						}

						asDraft = experimentsAPI.save(asDraft, APILocator.systemUser());

						if(experiment.status()==Status.RUNNING) {
							experimentsAPI.forceStart(asDraft.id().orElseThrow(),
									APILocator.systemUser(), schedulingOptional.get());
						} else {
							experimentsAPI.forceScheduled(asDraft.id().orElseThrow(),
									APILocator.systemUser(), schedulingOptional.get());
						}

					} else if(experiment.status()==Status.ENDED && localExperiment.isPresent()
							&& localExperiment.get().status()==Status.RUNNING) {
						experimentsAPI.end(localExperiment.orElseThrow().id().orElseThrow(),
									APILocator.systemUser());
					} else {
						experimentsAPI.save(experiment, APILocator.systemUser());
					}
				}
			}
    	} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when processing Experiment in '%s' with Id '%s': %s",
					workingOn, experiment.name(),
							experiment.id().orElseThrow(), e.getMessage());
			Logger.error(this.getClass(), errorMsg, e);
			throw new DotPublishingException(errorMsg, e);
    	}
    }

}

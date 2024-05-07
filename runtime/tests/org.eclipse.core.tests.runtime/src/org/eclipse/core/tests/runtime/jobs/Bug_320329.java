/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.time.Duration;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.harness.TestJob;
import org.junit.jupiter.api.Test;

/**
 * Regression test for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=320329.
 */
public class Bug_320329 extends AbstractJobTest {

	@Test
	public void testBug() {
		TestBarrier2 job2State = new TestBarrier2();
		Job j1 = new TestJob("job1", 10, 5);// 50 ms
		Job j2 = new TestJob("job2") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				job2State.upgradeTo(TestBarrier2.STATUS_RUNNING);
				return super.run(monitor);
			}
		};
		ISchedulingRule rule1 = new IdentityRule();
		ISchedulingRule rule2 = new IdentityRule();

		j1.setRule(rule1);
		j2.setRule(MultiRule.combine(rule1, rule2));
		j1.schedule();
		j2.schedule();
		job2State.waitForStatus(TestBarrier2.STATUS_RUNNING);

		// busy wait here
		Job.getJobManager().beginRule(rule2, new NullProgressMonitor());

		// Clean up
		Job.getJobManager().endRule(rule2);
		waitForCompletion(j1, Duration.ofMillis(60));
		waitForCompletion(j2, Duration.ofMillis(60));
	}
}

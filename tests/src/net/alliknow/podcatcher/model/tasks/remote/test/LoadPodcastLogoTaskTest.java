/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher.model.tasks.remote.test;

import android.graphics.Bitmap;
import android.test.InstrumentationTestCase;

import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastLogoTask;
import net.alliknow.podcatcher.model.test.Utils;
import net.alliknow.podcatcher.model.types.Podcast;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("javadoc")
public class LoadPodcastLogoTaskTest extends InstrumentationTestCase {

    private CountDownLatch signal = null;

    private List<Podcast> examplePodcasts;

    private class MockPodcastLogoLoader implements OnLoadPodcastLogoListener {

        protected Bitmap result;
        protected boolean failed;

        @Override
        public void onPodcastLogoLoaded(Podcast podcast) {
            this.result = podcast.getLogo();
            this.failed = false;

            signal.countDown();
        }

        @Override
        public void onPodcastLogoLoadFailed(Podcast podcast) {
            this.failed = true;

            signal.countDown();
        }
    }

    @Override
    protected void setUp() throws Exception {
        System.out.println("Set up test \"LoadPodcastLogo\" by loading example podcasts...");

        final Date start = new Date();
        examplePodcasts = Utils.getExamplePodcasts();

        System.out.println("Waited " + (new Date().getTime() - start.getTime())
                + "ms for example podcasts...");
    }

    public final void testLoadPodcastLogo() throws Throwable {
        MockPodcastLogoLoader mockLoader = new MockPodcastLogoLoader();

        int size = examplePodcasts.size();
        int index = 0;
        int failed = 0;

        // Actual example Podcast
        for (Podcast ep : examplePodcasts) {
            System.out.println("---- New Podcast (" + ++index + "/" + size + ") ----");
            System.out.println("Testing \"" + ep + "\"...");
            Podcast podcast = new Podcast(ep.getName(), ep.getUrl());
            podcast.parse(Utils.getParser(podcast));

            LoadPodcastLogoTask task = loadAndWait(mockLoader, podcast);

            if (mockLoader.failed) {
                System.out.println("Podcast " + ep.getName() + " failed!");
                failed++;
            } else {
                assertFalse(task.isCancelled());
                assertFalse(mockLoader.failed);
                assertNotNull(mockLoader.result);
                assertTrue(mockLoader.result.getByteCount() > 0);
            }

            System.out.println("Tested \"" + ep + "\" - okay...");
        }

        System.out.println("*** Tested all example podcast, failed on " + failed);
    }

    private LoadPodcastLogoTask loadAndWait(final MockPodcastLogoLoader mockLoader,
            final Podcast podcast) throws Throwable {
        final LoadPodcastLogoTask task = new LoadPodcastLogoTask(null, mockLoader);

        signal = new CountDownLatch(1);

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                task.execute(podcast);
            }
        });

        final Date start = new Date();
        signal.await();
        System.out.println("Waited " + (new Date().getTime() - start.getTime())
                + "ms for Podcast Logo \"" + podcast + "\"...");

        return task;
    }
}

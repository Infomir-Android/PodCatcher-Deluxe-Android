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

package net.alliknow.podcatcher.model.tasks;

/**
 * Class to indicate load progress.
 */
public class Progress {

    /** Flag indicating waiting state */
    private static final int PROGRESS_WAIT = -4;
    /** Flag indicating connection state */
    private static final int PROGRESS_CONNECT = -3;
    /** Flag indicating loading state */
    private static final int PROGRESS_LOAD = -2;
    /** Flag indicating parsing state */
    private static final int PROGRESS_PARSE = -1;

    /** Waiting state */
    public static final Progress WAIT = new Progress(PROGRESS_WAIT, -1);
    /** Connecting state */
    public static final Progress CONNECT = new Progress(PROGRESS_CONNECT, -1);
    /** Loading state */
    public static final Progress LOAD = new Progress(PROGRESS_LOAD, -1);
    /** Parsing state */
    public static final Progress PARSE = new Progress(PROGRESS_PARSE, -1);

    /** The actual amount of progress made */
    protected final int progress;
    /** The total amount of work */
    protected final int total;

    /**
     * Create new progress information.
     * 
     * @param progress Amount done.
     * @param total Amount to do in total.
     */
    public Progress(int progress, int total) {
        this.progress = progress;
        this.total = total;
    }

    /**
     * @return The amount of work already done.
     */
    public int getProgress() {
        return progress;
    }

    /**
     * @return The total amount of work.
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return The amount of work done in percent of total. Returns -1 if no
     *         valid percentage could be calculated.
     */
    public int getPercentDone() {
        if (total <= 0)
            return -1;
        else
            return (int) ((float) progress / (float) total * 100);
    }

    @Override
    public String toString() {
        return progress + "/" + total;
    }
}

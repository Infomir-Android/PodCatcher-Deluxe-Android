/** Copyright 2012 Kevin Hausmann
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
package net.alliknow.podcatcher.fragments;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.PodcastListAdapter;
import net.alliknow.podcatcher.fragments.AddPodcastFragment.AddPodcastListener;
import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask.PodcastLogoLoader;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask.PodcastLoader;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * List fragment to display the list of podcasts as part of the
 * podcast activity.
 * 
 * @author Kevin Hausmann
 */
public class PodcastListFragment extends ListFragment implements AddPodcastListener, PodcastLoader, PodcastLogoLoader {
	
	private AddPodcastFragment addPodcastFragment = new AddPodcastFragment();
	
	/** Container Activity must implement this interface */
    public interface OnPodcastSelectedListener {
    	/**
    	 * Updates the UI to reflect that a podcast has been selected.
    	 * @param selectedPodcast Podcast selected by the user
    	 */
    	public void onPodcastSelected(Podcast selectedPodcast);
    }
    /** The activity we are in (listens to user selection) */ 
    private OnPodcastSelectedListener selectedListener;
    
    /** Container Activity must implement this interface */
    public interface OnPodcastLoadedListener {
    	/**
    	 * Updates the UI to reflect that a podcast has been loaded.
    	 * @param loadedPodcast Podcast loaded
    	 */
    	public void onPodcastLoaded(Podcast loadedPodcast);
    	
    	/**
    	 * Notifies listener that a podcast load failed
    	 * @param failedPodcast Podcast that failed loading
    	 */
    	public void onPodcastLoadFailed(Podcast failedPodcast);
    }
    /** The activity we are in (listens to loading complete) */ 
    private OnPodcastLoadedListener loadedListener;
    
	/** The list of podcasts we know */
	private List<Podcast> podcastList = new ArrayList<Podcast>();
	/** Currently selected podcast */
	private Podcast currentPodcast;
	/** Currently show podcast logo */
	private Bitmap currentLogo;
	/** The name of the file we store our saved podcasts in (as OPML) */
	private static final String OPML_FILENAME = "podcasts.opml";
	/** The OPML file encoding */
	private static final String OPML_FILE_ENCODING = "utf8";
	
	/** The current podcast load task */
	private LoadPodcastTask loadPodcastTask;
	/** The current podcast logo load task */
	private LoadPodcastLogoTask loadPodcastLogoTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);
		// Loads podcasts from stored file to this.podcastList
		loadPodcastList();
		// Maps the podcast list items to the list UI
		setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
		// Make sure we are alerted if a new podcast is added
		addPodcastFragment.setAddPodcastListener(this);
		// If podcast list is empty we show dialog on startup
		if (getListAdapter().isEmpty()) addPodcastFragment.show(getFragmentManager(), "add_podcast");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.podcast_list, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		if (currentLogo != null) setPodcastLogo(currentLogo);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
       
        try {
            selectedListener = (OnPodcastSelectedListener) activity;
            loadedListener = (OnPodcastLoadedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPodcastSelectedListener");
        }
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.podcast_list_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_podcast_button) 
			addPodcastFragment.show(getFragmentManager(), "add_podcast");
				
		return item.getItemId() == R.id.add_podcast_button;
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Podcast selectedPodcast = podcastList.get(position);
		selectPodcast(selectedPodcast);
	}
	
	/**
	 * Notified by async RSS file loader on completion.
	 * Updates UI to display the podcast's episodes.
	 * @param podcast Podcast RSS feed loaded for
	 */
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		loadPodcastTask = null;
		
		// TODO Handle the case where the fragment is not attached because the activity is recreated
		if (loadedListener != null) loadedListener.onPodcastLoaded(podcast);
		else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
		
		// Download podcast logo
		if (podcast.getLogoUrl() != null) {
			loadPodcastLogoTask = new LoadPodcastLogoTask(this);
			loadPodcastLogoTask.execute(podcast);
		} else Log.d(getClass().getSimpleName(), "No logo for podcast " + podcast);
	}
	
	@Override
	public void onPodcastLogoLoaded(Bitmap logo) {
		loadPodcastLogoTask = null;
		currentLogo = logo;
		
		setPodcastLogo(logo);
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		loadPodcastTask = null;
		
		if (loadedListener != null) loadedListener.onPodcastLoadFailed(podcast);
		else Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listener attached");
		
		Log.w(getClass().getSimpleName(), "Podcast failed to load " + podcast);
	}
	
	@Override
	public void onPodcastLogoLoadFailed() {}
	
	@Override
	public void addPodcast(Podcast newPodcast) {
		if (! podcastList.contains(newPodcast)) {
			podcastList.add(newPodcast);
			Collections.sort(podcastList);
			
			setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
		} else Log.d(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName() + "\" is already in list.");
		
		selectPodcast(newPodcast);
		storePodcastList();
	}
	
	private void selectPodcast(Podcast selectedPodcast) {
		// Is this a valid selection (in podcast list and new)?
		if (podcastList.contains(selectedPodcast) && (currentPodcast == null || !currentPodcast.equals(selectedPodcast))) {
			currentPodcast = selectedPodcast;
			
			// Prepare UI
			((PodcastListAdapter) getListAdapter()).setSelectedPosition(podcastList.indexOf(selectedPodcast));
			setPodcastLogo(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.default_podcast_logo));
			selectedListener.onPodcastSelected(selectedPodcast);
			
			// Stopp loading previous tasks
			if (loadPodcastTask != null) loadPodcastTask.cancel(true);
			if (loadPodcastLogoTask != null) loadPodcastLogoTask.cancel(true);
						
			// Load if too old, otherwise just use previously loaded version
			if (selectedPodcast.needsReload()) {
				// Download podcast RSS feed (async)
				loadPodcastTask = new LoadPodcastTask(this);
				loadPodcastTask.execute(selectedPodcast);	
			}
			// Use buffered content
			else onPodcastLoaded(selectedPodcast);
		}
	}
	
	private void setPodcastLogo(Bitmap logo) {
		ImageView logoView = (ImageView) getView().findViewById(R.id.podcast_image);
		logoView.setImageBitmap(logo);
	}
	
	private void loadPodcastList() {
		//this is just for testing
		//if (! Arrays.asList(getActivity().fileList()).contains(OPML_FILENAME)) 
		//writeDummyPodcastList();
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			
			Document podcastFile = factory.newDocumentBuilder().parse(getActivity().openFileInput(OPML_FILENAME));
			NodeList podcasts = podcastFile.getElementsByTagName(OPML.OUTLINE);
			
			for (int index = 0; index < podcasts.getLength(); index++) {
				String name = podcasts.item(index).getAttributes().getNamedItem(OPML.TEXT).getNodeValue();
				String url = podcasts.item(index).getAttributes().getNamedItem(OPML.XMLURL).getNodeValue();
				
				podcastList.add(new Podcast(name, new URL(url)));
			}
			
			Collections.sort(podcastList); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void storePodcastList() {
		try {			
			BufferedWriter writer = getPodcastFileWriter();
			
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.write("<opml version=\"2.0\">");
			writer.write("<body>");
			
			for (Podcast podcast : podcastList) {
				String outline = "<outline text=\"" + podcast.getName() + "\" xmlUrl=\"" + podcast.getUrl() + "\" />";
				writer.write(outline);
			}
			
			writer.write("</body></opml>");
			writer.close();
			
			Log.d(getClass().getSimpleName(), "OPML podcast file written");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	

	private void writeDummyPodcastList() {
		try {
			BufferedWriter writer = getPodcastFileWriter();
			
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.write("<opml version=\"2.0\">");
			writer.write("<body>");
			writer.write("<outline text=\"This American Life\" xmlUrl=\"http://feeds.thisamericanlife.org/talpodcast\"/>");
			writer.write("<outline text=\"Radiolab\" xmlUrl=\"http://feeds.wnyc.org/radiolab\"/>");
			writer.write("<outline text=\"Linux Outlaws\" xmlUrl=\"http://feeds.feedburner.com/linuxoutlaws\"/>");
			writer.write("<outline text=\"GEO\" xmlUrl=\"http://www.geo.de/GEOaudio/index.xml\"/>");
			writer.write("</body></opml>");
			writer.close();
			
			Log.d(getClass().getSimpleName(), "Dummy OPML written");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private BufferedWriter getPodcastFileWriter() throws FileNotFoundException,	UnsupportedEncodingException {
		FileOutputStream fos = getActivity().openFileOutput(OPML_FILENAME, Context.MODE_PRIVATE);
		
		return new BufferedWriter(new OutputStreamWriter(fos, OPML_FILE_ENCODING));
	}
}

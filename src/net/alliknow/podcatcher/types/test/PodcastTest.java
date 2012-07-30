package net.alliknow.podcatcher.types.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.alliknow.podcatcher.types.Podcast;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class PodcastTest {

	@Test
	public final void testGetName() {
		String name = null;
		Podcast podcast = new Podcast(name, null);
		assertEquals(name, podcast.getName());
		
		name = "";
		podcast = new Podcast(name, null);
		assertEquals(name, podcast.getName());
		
		name = "Test";
		podcast = new Podcast(name, null);
		assertEquals(name, podcast.getName());
	}

	@Test
	public final void testToString() {
		String name = null;
		Podcast podcast = new Podcast(name, null);
		assertNotNull(podcast.toString());
		
		name = null;
		podcast = new Podcast(name, null);
		assertEquals("Unnamed podcast", podcast.toString());
		
		name = "My Podcast";
		podcast = new Podcast(name, null);
		assertEquals(name, podcast.toString());
	}
	
	@Test
	public final void testGetEpisodes() {
		Podcast podcast = new Podcast(null, null);
		assertNotNull(podcast.getEpisodes());
		assertEquals(0, podcast.getEpisodes().size());
		
		try {
			podcast = new Podcast("Linux Outlaws", new URL("http://feeds.feedburner.com/linuxoutlaws"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		podcast.setRssFile(loadRssFile(podcast));
		assertNotNull(podcast.getEpisodes());
		assertEquals(10, podcast.getEpisodes().size());
		
		try {
			podcast = new Podcast("This American Life", new URL("http://feeds.thisamericanlife.org/talpodcast"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		podcast.setRssFile(loadRssFile(podcast));
		assertNotNull(podcast.getEpisodes());
		assertEquals(1, podcast.getEpisodes().size());
		
		try {
			podcast = new Podcast("Radiolab", new URL("http://feeds.wnyc.org/radiolab"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		podcast.setRssFile(loadRssFile(podcast));
		assertNotNull(podcast.getEpisodes());
		assertTrue(podcast.getEpisodes().size() > 0);
		assertTrue(podcast.getEpisodes().get(0).getName() != null);
		assertTrue(podcast.getEpisodes().get(0).getName().length() > 0);
	}
	
	private Document loadRssFile(Podcast podcast) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(podcast.getUrl().openStream());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}

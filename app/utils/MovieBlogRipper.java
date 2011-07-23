/**
 * 
 */
package utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import models.DownloadLink;
import models.MovieBlogPage;
import models.Post;
import models.Tag;

import org.apache.commons.collections.list.TreeList;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import play.Logger;
import play.Play;
import play.libs.Codec;
import play.libs.WS;
import play.libs.WS.HttpResponse;

/**
 * @author prime
 * 
 */
public class MovieBlogRipper {

	static TreeList	urllist	= new TreeList();

	public static void start() {
//		 fetchYesterDaysArchive();
//		
//		 createPostStubs();
//		
		 fetchPosts(1);

		createPosts();
	}

	private static void createPosts() {
		List<MovieBlogPage> mbPageList = MovieBlogPage.find("reindex = ? and post = ? and postCreated = ?", false, true, false).fetch();
		for (MovieBlogPage mbPage : mbPageList) {
			Post post = Post.find("byUrl", mbPage.url).first();

			if (post == null) {
				post = new Post();
			}

			String content = mbPage.content;

			// url
			Logger.info("Url: %s", mbPage.url);
			post.url = mbPage.url;

			// title
			Pattern pattern = Pattern.compile("<h2 id=\"post-.*?<a.*?>(.*?)</a>");
			Matcher matcher = pattern.matcher(content);

			while (matcher.find()) {
				Logger.info("Title: %s", cleanTitle(matcher.group(1)));
				post.title = cleanTitle(matcher.group(1));
			}

			// get the main content
			String entry = fetchBetweenTags(content, "<!-- the entry -->", "<!-- You can start editing here. -->", false, false, false, false);

			// get details
			String details = fetchBetweenTags(entry, "<blockquote>", "</blockquote>", false, false, false, false);
			details = stripHtml(details.replace("\n", "###"));
			details = details.replace("&times;", "x");

			String[] lines = details.split("###");
			int videocnt = 1;
			int audiocnt = 1;

			for (String string : lines) {
				string = string.trim();

				if (string.toLowerCase().contains("video") || string.toLowerCase().contains("resolution") || string.toLowerCase().contains("auflösung") || string.toLowerCase().contains("fps")) {
					string = string.split(":")[1];
					// System.out.println(videocnt + " - " +string);
					if (videocnt == 1) {
						post.video1 = string;
						videocnt++;
					} else if (videocnt == 2) {
						post.video2 = string;
						videocnt++;
					} else if (videocnt == 3) {
						post.video3 = string;
						videocnt++;
					} else if (videocnt == 4) {
						post.video4 = string;
						videocnt++;
					}
				}

				if (string.toLowerCase().contains("audio")) {
					string = string.split(":")[1];
					// System.out.println(audiocnt + " - " +string);
					if (audiocnt == 1) {
						post.audio1 = string;
						audiocnt++;
					} else if (audiocnt == 2) {
						post.audio2 = string;
						audiocnt++;
					} else if (audiocnt == 3) {
						post.audio3 = string;
						audiocnt++;
					} else if (audiocnt == 4) {
						post.audio4 = string;
						audiocnt++;
					}
				}

			}

			// content
			String artikel = fetchBetweenTags(content, "<!-- the entry -->", "<blockquote>", false, false, false, false);
			// System.out.println(stripHtml(artikel));
			post.content = stripHtml(artikel);

			// img
			pattern = Pattern.compile("<img src=(.*?)>");
			matcher = pattern.matcher(entry);

			while (matcher.find()) {

				String[] testUrls = matcher.group(1).replace("\"", "").split(" ");
				URL url = null;
				for (String testUrl : testUrls) {
					try {
						url = new URL(testUrl);
					} catch (MalformedURLException e) {
						// clear malformed urls
					}
				}
				post.imgUrl = url.toString();
				Logger.info("Image Url: %s", post.imgUrl);
			}

			// save image
			savePostImage(post.imgUrl, post);

			// System.out.println(entry);

			// fileinfos
			String fileinfos = fetchBetweenTags(entry, "<strong>", "</strong>", false, true, true, true);

			// downloadlinks
			pattern = Pattern.compile("<strong>(.*?)</strong><a href=(.*?)>(.*?)</a>");
			matcher = pattern.matcher(fileinfos);
			while (matcher.find()) {
				String[] testUrls = matcher.group(2).replace("\"", "").replace("'", "").split(" ");
				URL url = null;
				for (String testUrl : testUrls) {
					try {
						url = new URL(testUrl);
					} catch (MalformedURLException e) {
						// clear malformed urls
					}
				}

				String type = matcher.group(1).toLowerCase();
				String link = matcher.group(3);
				if (type.contains("download") || type.contains("mirror")) {
					// DownloadLink dl = new DownloadLink();
					DownloadLink dl = DownloadLink.find("url = ? and post_id = ?", url.toString(), post.id).first();
					if (dl == null) {
						dl = new DownloadLink();
						dl.post = post;
						dl.provider = link;
						dl.url = url.toString();
						post.save();
						dl.save();
					}
				}
			}

			// info links
			pattern = Pattern.compile("\\| <a href=(.*?)>(.*?)</a>");
			matcher = pattern.matcher(fileinfos);
			while (matcher.find()) {
				String[] testUrls = matcher.group(1).replace("\"", "").replace("'", "").split(" ");
				URL url = null;
				for (String testUrl : testUrls) {
					try {
						url = new URL(testUrl);
					} catch (MalformedURLException e) {
						// clear malformed urls
					}
				}

				String link = matcher.group(2).toLowerCase();
				if (link.toLowerCase().contains("imdb")) {
					Pattern subpattern = Pattern.compile("(tt[0-9]+)");
					Matcher submatcher = subpattern.matcher(url.toString());
					while (submatcher.find()) {
						post.imdbID = matcher.group(1);
					}
				}

				if (link.toLowerCase().contains("nfo")) {
					// post.nfo = url.toString();
				}
				if (link.toLowerCase().contains("sample")) {
					post.sampleUrl = url.toString();
				}
			}

			// infos
			pattern = Pattern.compile("<strong>(.*?)</strong>(.*?)(\\||<br />)");
			matcher = pattern.matcher(fileinfos);

			while (matcher.find()) {
				String info = matcher.group(2);
				String link = matcher.group(1);

				if (link.toLowerCase().contains("dauer")) {
					Pattern subpattern = Pattern.compile("([0-9]+)");
					Matcher submatcher = subpattern.matcher(info);
					while (submatcher.find()) {
						post.length = Long.parseLong(submatcher.group(1));
					}
				}

				if (link.toLowerCase().contains("größe")) {
					Pattern subpattern = Pattern.compile("([0-9]+)");
					Matcher submatcher = subpattern.matcher(info);
					while (submatcher.find()) {
						post.size = Long.parseLong(submatcher.group(1));
					}
				}

				if (link.toLowerCase().contains("format")) {
					post.format = info.trim();
				}

			}

			
			
			pattern = Pattern.compile("<strong>Passwort: </strong>(.*?)<strong>");
			matcher = pattern.matcher(fileinfos);

			while (matcher.find()) {
				post.password = matcher.group(1);
			}
			
			
			// postinfos
			String postinfos = fetchBetweenTags(entry, "<div id=\"info\">", "</div>", false, true, false, false);
			
			pattern = Pattern.compile("Datum: (.*?)<br />");
			matcher = pattern.matcher(postinfos);

			while (matcher.find()) {
				String dateString = matcher.group(1);
				SimpleDateFormat sdfmt = new SimpleDateFormat(); 
				sdfmt.applyPattern( "EEEE', 'dd. MMMM yyyy HH:mm" ); 
				try {
					post.posted = sdfmt.parse(dateString);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//tags
			try {
				post.tags.clear();
			} catch (NullPointerException e) {
				post.tags = new TreeSet<Tag>();
				System.out.println("test");
			}
			
			pattern = Pattern.compile("rel=\"category.*?\">(.*?)</a>");
			matcher = pattern.matcher(postinfos);

			while (matcher.find()) {
				String tag = matcher.group(1);
				post.tags.add(Tag.findOrCreateByName(tag));
				post.save();
			}

			//bitrate 1598 kb
			//System.out.println(entry);
			pattern = Pattern.compile("video.*?([0-9]+).kb",Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(entry);

			while (matcher.find()) {
				String bitrate = matcher.group(1);
				post.bitrate = new Long(bitrate);
			}
			
			//video
			entry = entry.replace("&#215;", "x");
			pattern = Pattern.compile("((\\d{1,5})[x|×](\\d{1,5}))",Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(entry);

			while (matcher.find()) {
				String videox = matcher.group(2);
				String videoy = matcher.group(3);
				post.videox = new Long(videox);
				post.videoy = new Long(videoy);
				post.resolution = matcher.group(1);
			}
			mbPage.postCreated = true;
			mbPage.save();
			post.save();
		}

	}

	private static void savePostImage(String imgUrl, Post post) {
		String imgName = Codec.hexMD5(imgUrl);
		File imgPath = new File(Play.applicationPath + File.separator + "public" + File.separator + "images" + File.separator + "cache");

		if (!imgPath.exists()) {
			if (!imgPath.mkdirs()) {
				Logger.fatal("Error while creating path for %s", imgPath);
			}
		}
		File imgFile = new File(imgPath.toString() + File.separator + imgName);

		if (imgFile.exists()) {
			Post copyPost = Post.find("byImageCache", imgFile.toString()).first();
			if (copyPost != null) {
				post.imageApplicationType = copyPost.imageApplicationType;
				post.imageType = copyPost.imageType;
				post.imageCache = copyPost.imageCache;
				post.imagesaved = true;
				post.save();
				return;
			}
		}

		Logger.info("Fetching Post from %s...", imgUrl);
		HttpResponse httpResponse = null;
		try {
			httpResponse = WS.url(imgUrl).get();
			String formatName = httpResponse.getContentType();
			Logger.info("Creating %s %s...", formatName, imgFile);
			if (!ImageIO.write(ImageIO.read(httpResponse.getStream()), formatName.split("/")[1], imgFile)) {
				Logger.fatal("Error while creating image %s", imgFile);
			} else {
				post.imageApplicationType = formatName;
				post.imageType = formatName.split("/")[1];
				post.imageCache = imgFile.toString();
				post.imagesaved = true;
				post.save();
			}
		} catch (Exception e) {
			Logger.info("Error while fetching %s, will try it aggain on next run", imgUrl);
			post.imagesaved = false;
			post.save();
		}

	}

	/**
	 * @param content
	 * @return
	 */
	public static String fetchBetweenTags(String content, String startTag, String endTag, Boolean stripHtml, Boolean lastorfirst, Boolean stripStartTag, Boolean stripEndTag) {

		String between = null;
		try {
			if (stripStartTag) {
				between = content.substring(content.indexOf(startTag));
			} else {
				between = content.substring(content.indexOf(startTag) + startTag.length());
			}

			int endTagLength = 0;
			if (stripEndTag) {
				endTagLength = endTag.length();
			}

			if (lastorfirst) {
				between = between.substring(0, between.lastIndexOf(endTag) + endTagLength);
			} else {
				between = between.substring(0, between.indexOf(endTag) + endTagLength);
			}

			if (stripHtml) {
				Whitelist whitelist = Whitelist.simpleText();
				return Jsoup.clean(between, whitelist);
			}
		} catch (StringIndexOutOfBoundsException e) {
			// TODO: handle exception
		}
		return between;
	}

	private static String stripHtml(String replace) {
		Whitelist whitelist = Whitelist.none();
		replace = Jsoup.clean(replace, whitelist);
		return replace;
	}

	private static String cleanContent(String content) {
		Whitelist whitelist = Whitelist.simpleText();
		content = Jsoup.clean(content, whitelist);
		return content;
	}

	private static String cleanTitle(String title) {
		title = title.replace(".", " ");
		return title;
	}

	/**
	 * 
	 */
	@SuppressWarnings("unused")
	public static void fetchYesterDaysArchive() {
		int startPage = 2;
		int endPage = 0;
		LocalDate date = new LocalDate();
		date = date.minusDays(1);
		DateTimeFormatter fmt = DateTimeFormat.forPattern("/yyyy/MM/dd/");
		String baseurl = "http://www.movie-blog.org" + date.toString(fmt);

		Logger.info("Find Post from %s...", baseurl);

		MovieBlogPage mbPage = MovieBlogPage.find("byUrl", baseurl).first();
		String site = "";
		if (mbPage == null) {
			mbPage = new MovieBlogPage();
			HttpResponse httpResponse = WS.url(baseurl).get();
			site = httpResponse.getString();
			mbPage.content = site;
			mbPage.post = false;
			mbPage.indexed = new Date(System.currentTimeMillis());
			mbPage.reindex = false;
			mbPage.postCreated = false;
			mbPage.url = baseurl;
			mbPage.save();
		}
		site = mbPage.content;

		Pattern pattern = Pattern.compile("(<h1 id=\"post.*<a href=\")(.*?)(\")");
		Matcher matcher = pattern.matcher(site);

		while (matcher.find()) {
			urllist.add(matcher.group(2));
		}

		pattern = Pattern.compile("<span class=\'pages\'>Seite (.*?) von (.*?)</span>");
		matcher = pattern.matcher(site);

		while (matcher.find()) {
			startPage = Integer.parseInt(matcher.group(1));
			endPage = Integer.parseInt(matcher.group(2));
		}

		for (int i = 2; i <= endPage; i++) {
			String pageUrl = baseurl + "page/" + i + "/";
			fetchPage(pageUrl);
		}
	}

	private static void fetchPosts(int fetchsize) {
		List<MovieBlogPage> mbPageList = MovieBlogPage.find("byReindex", true).fetch(fetchsize);
		for (MovieBlogPage mbPage : mbPageList) {
			Logger.info("Fetching Post from %s...", mbPage.url);
			String site = null;
			HttpResponse httpResponse = null;
			try {
				httpResponse = WS.url(mbPage.url).get();
				site = httpResponse.getString();
				mbPage.content = site;
				mbPage.post = true;
				mbPage.indexed = new Date(System.currentTimeMillis());
				mbPage.reindex = false;
				mbPage.postCreated = false;
				mbPage.save();
			} catch (Exception e) {
				Logger.info("Error while fetching %s, will try it aggain on next run", mbPage.url);
			}
		}
	}

	private static void createPostStubs() {
		for (Object obUrl : urllist) {
			String postUrl = (String) obUrl;
			MovieBlogPage mbPage = MovieBlogPage.find("byUrl", postUrl).first();
			if (mbPage == null) {
				Logger.info("Creating Post from %s, will fetch it per Job", postUrl);
				mbPage = new MovieBlogPage();
				mbPage.content = null;
				mbPage.post = false;
				mbPage.indexed = null;
				mbPage.reindex = true;
				mbPage.postCreated = false;
				mbPage.url = postUrl;
				mbPage.save();
			} else {
				Logger.info("Found %s in the Database, no refetch", postUrl);
			}
		}
	}

	private static void fetchPage(String pageUrl) {
		MovieBlogPage mbPage = MovieBlogPage.find("byUrl", pageUrl).first();

		if (mbPage != null && mbPage.reindex) {
			mbPage.delete();
			mbPage = null;
		} else {
			Logger.info("Found %s in the Database, no refetch", pageUrl);
		}

		String site = null;
		HttpResponse httpResponse = null;
		if (mbPage == null) {
			try {
				Logger.info("Fetching %s...", pageUrl);
				httpResponse = WS.url(pageUrl).get();
				site = httpResponse.getString();
				mbPage = new MovieBlogPage();
				mbPage.content = site;
				mbPage.post = false;
				mbPage.indexed = new Date(System.currentTimeMillis());
				mbPage.reindex = false;
				mbPage.postCreated = false;
				mbPage.url = pageUrl;
				mbPage.save();
			} catch (Exception e) {
				Logger.info("Error while fetching %s, will try it aggain on next run", pageUrl);
				mbPage = new MovieBlogPage();
				mbPage.content = site;
				mbPage.post = false;
				mbPage.postCreated = false;
				mbPage.indexed = null;
				mbPage.reindex = true;
				mbPage.url = pageUrl;
				mbPage.save();
			}
		}
		site = mbPage.content;
		Pattern pattern = Pattern.compile("(<h1 id=\"post.*<a href=\")(.*?)(\")");
		Matcher matcher = pattern.matcher(site);
		while (matcher.find()) {
			urllist.add(matcher.group(2));
		}
	}
}

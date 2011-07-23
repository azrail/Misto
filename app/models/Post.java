/**
 * 
 */
package models;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author prime
 * 
 */
@Entity
public class Post extends Model {
	@Required
	public String				url;
	@Required
	public String				imgUrl;
	public String				imageCache;
	public String				imageApplicationType;
	public String				imageType;
	public Boolean				imagesaved;

	@Required
	public Date					posted;

	@Lob
	@Required
	@MaxSize(1000)
	public String				title;

	@Lob
	@Required
	@MaxSize(10000)
	public String				content;

	public String				audio1;
	public String				audio2;
	public String				audio3;
	public String				audio4;

	public String				video1;
	public String				video2;
	public String				video3;
	public String				video4;

	public Long					size;

	public Long					videox;
	public Long					videoy;
	public String				resolution;
	public String				format;
	public String				sampleUrl;
	public Long					length;
	public Long					bitrate;
	public String				imdbID;

	@Lob
	@Required
	@MaxSize(100000)
	public String				nfo;
	public String				password;

	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
	public List<DownloadLink>	downloads;

	@ManyToMany(cascade = CascadeType.PERSIST)
	public Set<Tag>				tags;
}

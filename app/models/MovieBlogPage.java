/**
 * 
 */
package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Lob;

import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author prime
 * 
 */
@Entity
public class MovieBlogPage extends Model {
	@Required
	public String	url;

	@Required
	public Date		posted;

	@Required
	public Date		indexed;

	@Required
	public Boolean	reindex;

	@Required
	public Boolean	post;
	
	@Required
	public Boolean	postCreated;
	
	@Lob
	@Required
	@MaxSize(100000)
	public String	content;
}

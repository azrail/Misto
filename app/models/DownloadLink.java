/**
 * 
 */
package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author prime
 *
 */
@Entity
public class DownloadLink extends Model {
	@Required
	public String	provider;
	
	@Required
	public String	url;
	
	@ManyToOne
	@Required
	public Post		post;
	
}

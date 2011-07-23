package controllers;

import play.*;
import play.mvc.*;
import utils.MovieBlogRipper;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
    	MovieBlogRipper.start();
    	
        render();
    }

}
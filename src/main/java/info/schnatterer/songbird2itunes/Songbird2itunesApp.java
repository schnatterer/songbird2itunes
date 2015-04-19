package info.schnatterer.songbird2itunes;

import info.schnatterer.itunes4j.exception.ITunesException;

import java.sql.SQLException;

/**
 * Entry point for the songbird2Itunes Command Line Application
 */
public class Songbird2itunesApp {
	public static void main(String[] args) throws SQLException, ITunesException {
		// TODO implement a CLI here
		new Songbird2itunes().convert(args[0]);
	}
}

package com.github.sardine;

import java.util.logging.Logger;

/**
 * Provides version information from the manifest.
 *
 * @author Jeff Schnitzer
 */
public final class Version
{
    private static final Logger logger = Logger.getLogger(Version.class.getName());

    private Version() {}

	/**
	 * @return The <code>Specification-Version</code> in the JAR manifest.
	 */
	public static String getSpecification()
	{
		Package pkg = Version.class.getPackage();
		return (pkg == null) ? null : pkg.getSpecificationVersion();
	}

	/**
	 * @return The <code>Implementation-Version</code> in the JAR manifest.
	 */
	public static String getImplementation()
	{
		Package pkg = Version.class.getPackage();
		return (pkg == null) ? null : pkg.getImplementationVersion();
	}

	/**
	 * A simple main method that prints the version and exits
	 */
	public static void main(String[] args)
	{
		logger.info("Version: " + getSpecification());
		logger.info("Implementation: " + getImplementation());
	}
}

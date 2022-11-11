package megan.accessiondb;

/**
 * configuration requests for the SQLite database connections when working with mapping DBs
 * Daniel Huson, 11.22
 */
public class ConfigRequests {
	private static boolean useTempStoreInMemory=false;
	private static int cacheSize=-10000;

	/**
	 * use temp store in memory when creating a mapping DB?
	 * @return
	 */
	public static boolean isUseTempStoreInMemory() {
		return useTempStoreInMemory;
	}

	/**
	 * determine whether to request in-memory temp storage. If not requested, default will be used
	 * @param useTempStoreInMemory
	 */
	public static void setUseTempStoreInMemory(boolean useTempStoreInMemory) {
		ConfigRequests.useTempStoreInMemory = useTempStoreInMemory;
	}

	/**
	 * cache size to use
	 * @return
	 */
	public static int getCacheSize() {
		return cacheSize;
	}

	/**
	 * set the SQLite cache size. Negative values have a special meaning, see SQLite documentation
	 * @param cacheSize
	 */
	public static void setCacheSize(int cacheSize) {
		ConfigRequests.cacheSize = cacheSize;
	}
}

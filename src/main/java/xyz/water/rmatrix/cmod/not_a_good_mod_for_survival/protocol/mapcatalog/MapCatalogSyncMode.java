package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

/** Wire-order-sensitive synchronization modes defined by MapCatalogSync. */
public enum MapCatalogSyncMode {
    FULL,
    DELTA,
    NO_CHANGE,
    DENIED
}

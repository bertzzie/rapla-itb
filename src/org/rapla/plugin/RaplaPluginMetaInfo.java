package org.rapla.plugin;




/** Constant Pool of all meta infos Rapla system */
public interface RaplaPluginMetaInfo
{

    /** return Boolean.TRUE in the  getPluginMetaInfos( String key ) to enable your plugin by default in the plugin options method */
    String METAINFO_PLUGIN_ENABLED_BY_DEFAULT = "org.rapla.framework.EnablePluginByDefault";

}

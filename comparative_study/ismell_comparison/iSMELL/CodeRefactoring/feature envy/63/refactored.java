public class TiVoServiceManager {
    private TiVo tivo;

    public TiVoServiceManager() {
        tivo = new TiVo();
    }

    public void setTiVoInfo(String name, String type, ServiceInfo info) {
        tivo.setName(name.substring(0, name.length() - (type.length() + 1)));
        tivo.setServer(info.getServer());
        tivo.setPort(info.getPort());
        tivo.setAddress(info.getAddress());
    }

    public void setTiVoProperties(ServiceInfo info) {
        for (Enumeration names = info.getPropertyNames(); names.hasMoreElements();) {
            String prop = (String) names.nextElement();
            if (prop.equals(TIVO_PLATFORM)) {
                tivo.setPlatform(info.getPropertyString(prop));
            } else if (prop.equals(TIVO_TSN)) {
                tivo.setServiceNumber(info.getPropertyString(prop));
            } else if (prop.equals(TIVO_SW_VERSION)) {
                tivo.setSoftwareVersion(info.getPropertyString(prop));
            } else if (prop.equals(TIVO_PATH)) {
                tivo.setPath(info.getPropertyString(prop));
            }
        }
    }

    public TiVo getTiVo() {
        return tivo;
    }
}

    public void resolveService(JmDNS jmdns, String type, String name, ServiceInfo info) {
        log.debug("resolveService: " + type + " (" + name + ")");
        if (type.equals(HTTP_SERVICE)) {
            if (info == null) {
                log.error("Service not found: " + type + "(" + name + ")");
            } else {
                boolean found = info.getPropertyString(TIVO_PLATFORM)!=null && info.getPropertyString(TIVO_TSN)!=null;
                TiVoServiceManager manager = new TiVoServiceManager();
                manager.setTiVoInfo(name, type, info);
                manager.setTiVoProperties(info);
                TiVo tivo = manager.getTiVo();
                if (found) {
                    List<TiVo> tivos = Server.getServer().getTiVos();
                    handleTiVoUpdates(tivo, tivos);
                }
            }
        }
    }

    public void handleTiVoUpdates(TiVo tivo, List<TiVo> tivos) {
        boolean matched = false;
        Iterator<TiVo> iterator = tivos.iterator();
        while (iterator.hasNext()) {
            TiVo knownTiVo = iterator.next();
            if (knownTiVo.getAddress().equals(tivo.getAddress())) {
                matched = true;
                boolean modified = false;
                // set knownTiVo properties and check if modified
                if (modified)
                    Server.getServer().updateTiVos(tivos);
            }
        }
        if (!matched) {
            tivos.add(tivo);
            Server.getServer().updateTiVos(tivos);
            log.info("Found TiVo: " + tivo.toString());
        }
    }
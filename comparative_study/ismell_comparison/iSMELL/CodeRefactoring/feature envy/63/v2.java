public class TiVoServiceResolver {
    private JmDNS jmdns;
    private List<TiVo> tivos;

    public TiVoServiceResolver(JmDNS jmdns) {
        this.jmdns = jmdns;
        this.tivos = Server.getServer().getTiVos();
    }

    public void resolveService(String type, String name, ServiceInfo info) {
        if (!HTTP_SERVICE.equals(type)) {
            return;
        }

        if (info == null) {
            log.error("Service not found: " + type + "(" + name + ")");
            return;
        }

        TiVo tivo = createTiVoFromServiceInfo(name, type, info);
        if (tivo.hasValidProperties()) {
            updateTiVoList(tivo);
        }
    }

    private TiVo createTiVoFromServiceInfo(String name, String type, ServiceInfo info) {
        TiVo tivo = new TiVo();
        tivo.setName(extractTiVoName(name, type));
        tivo.setServer(info.getServer());
        tivo.setPort(info.getPort());
        tivo.setAddress(info.getAddress());
        setTiVoProperties(tivo, info);
        return tivo;
    }

    private String extractTiVoName(String name, String type) {
        return name.substring(0, name.length() - (type.length() + 1));
    }

    private void setTiVoProperties(TiVo tivo, ServiceInfo info) {
        for (Enumeration names = info.getPropertyNames(); names.hasMoreElements();) {
            String prop = (String) names.nextElement();
            String value = info.getPropertyString(prop);
            switch (prop) {
                case TIVO_PLATFORM:
                    tivo.setPlatform(value);
                    break;
                case TIVO_TSN:
                    tivo.setServiceNumber(value);
                    break;
                case TIVO_SW_VERSION:
                    tivo.setSoftwareVersion(value);
                    break;
                case TIVO_PATH:
                    tivo.setPath(value);
                    break;
            }
        }
    }

    private void updateTiVoList(TiVo newTiVo) {
        boolean matched = false;
        for (TiVo knownTiVo : tivos) {
            if (knownTiVo.getAddress().equals(newTiVo.getAddress())) {
                matched = true;
                updateKnownTiVo(knownTiVo, newTiVo);
            }
        }
        if (!matched) {
            tivos.add(newTiVo);
            log.info("Found TiVo: " + newTiVo.toString());
        }
        Server.getServer().updateTiVos(tivos);
    }

    private void updateKnownTiVo(TiVo knownTiVo, TiVo newTiVo) {
        boolean modified = false;
        modified |= knownTiVo.updateIfDifferent(knownTiVo::getPlatform, newTiVo::getPlatform, knownTiVo::setPlatform);
        modified |= knownTiVo.updateIfDifferent(knownTiVo::getServiceNumber, newTiVo::getServiceNumber, knownTiVo::setServiceNumber);
        modified |= knownTiVo.updateIfDifferent(knownTiVo::getSoftwareVersion, newTiVo::getSoftwareVersion, knownTiVo::setSoftwareVersion);
        modified |= knownTiVo.updateIfDifferent(knownTiVo::getPath, newTiVo::getPath, knownTiVo::setPath);
        modified |= knownTiVo.updateIfDifferent(knownTiVo::getServer, newTiVo::getServer, knownTiVo::setServer);
        modified |= knownTiVo.updateIfDifferent(() -> knownTiVo.getPort(), () -> newTiVo.getPort(), knownTiVo::setPort);

        if (modified) {
            log.info("Updated TiVo: " + knownTiVo.toString());
        }
    }
}

    // Usage in the original method
    public void resolveService(JmDNS jmdns, String type, String name, ServiceInfo info) {
        log.debug("resolveService: " + type + " (" + name + ")");
        TiVoServiceResolver resolver = new TiVoServiceResolver(jmdns);
        resolver.resolveService(type, name, info);
    }
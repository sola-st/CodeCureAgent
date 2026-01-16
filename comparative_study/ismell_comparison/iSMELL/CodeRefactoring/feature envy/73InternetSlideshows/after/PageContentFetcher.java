package org.lnicholls.galleon.apps.internetSlideshows;



import java.awt.image.*;

import java.awt.*;

import java.io.ByteArrayInputStream;

import java.io.File;

import java.io.FileInputStream;

import java.io.StringReader;

import java.net.URL;

import java.text.ParsePosition;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Arrays;

import java.util.Date;

import java.util.Iterator;

import java.util.List;

import java.util.Random;

import java.util.StringTokenizer;

import java.util.Vector;



import javax.imageio.ImageIO;



import net.sf.hibernate.HibernateException;

import net.sf.hibernate.Query;

import net.sf.hibernate.ScrollableResults;

import net.sf.hibernate.Session;

import net.sf.hibernate.Transaction;



import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;

import org.dom4j.Document;

import org.dom4j.Element;

import org.dom4j.io.SAXReader;

import org.lnicholls.galleon.app.AppContext;

import org.lnicholls.galleon.app.AppFactory;

import org.lnicholls.galleon.apps.internetSlideshows.InternetSlideshowsOptionsPanel.ImagesWrapper;

import org.lnicholls.galleon.database.Audio;

import org.lnicholls.galleon.database.AudioManager;

import org.lnicholls.galleon.database.HibernateUtil;

import org.lnicholls.galleon.database.PersistentValue;

import org.lnicholls.galleon.database.PersistentValueManager;

import org.lnicholls.galleon.database.Podcast;

import org.lnicholls.galleon.database.PodcastTrack;

import org.lnicholls.galleon.media.ImageManipulator;

import org.lnicholls.galleon.media.JpgFile;

import org.lnicholls.galleon.media.MediaManager;

import org.lnicholls.galleon.media.Mp3File;

import org.lnicholls.galleon.server.MusicPlayerConfiguration;

import org.lnicholls.galleon.server.Server;

import org.lnicholls.galleon.util.Effect;

import org.lnicholls.galleon.util.Effects;

import org.lnicholls.galleon.util.FileFilters;

import org.lnicholls.galleon.util.FileSystemContainer;

import org.lnicholls.galleon.util.NameValue;

import org.lnicholls.galleon.util.Tools;

import org.lnicholls.galleon.util.FileSystemContainer.FileItem;

import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;

import org.lnicholls.galleon.util.FileSystemContainer.Item;

import org.lnicholls.galleon.widget.DefaultApplication;

import org.lnicholls.galleon.widget.DefaultMenuScreen;

import org.lnicholls.galleon.widget.DefaultOptionList;

import org.lnicholls.galleon.widget.DefaultOptionsScreen;

import org.lnicholls.galleon.widget.DefaultScreen;

import org.lnicholls.galleon.widget.Grid;

import org.lnicholls.galleon.widget.LabelText;

import org.lnicholls.galleon.widget.MusicOptionsScreen;

import org.lnicholls.galleon.widget.OptionsButton;

import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

import org.lnicholls.galleon.widget.DefaultApplication.VersionScreen;



import com.tivo.hme.bananas.BEvent;

import com.tivo.hme.bananas.BHighlights;

import com.tivo.hme.bananas.BList;

import com.tivo.hme.bananas.BText;

import com.tivo.hme.bananas.BView;

import com.tivo.hme.sdk.Resource;

import com.tivo.hme.sdk.View;

import com.tivo.hme.interfaces.IContext;

import com.tivo.hme.interfaces.IArgumentList;



import de.nava.informa.core.ChannelBuilderIF;

import de.nava.informa.core.ChannelIF;

import de.nava.informa.core.ItemIF;

import de.nava.informa.impl.basic.ChannelBuilder;

import de.nava.informa.parsers.FeedParser;

public class PageContentFetcher {
    public String fetchPageContent(String url) {
        PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(InternetSlideshows.class.getName() + "." + url);
        String content = persistentValue == null ? null : persistentValue.getValue();
        if (PersistentValueManager.isAged(persistentValue)) {
            try {
                String page = Tools.getPage(new URL(url));
                if (page != null && page.length() > 0) {
                    content = page;
                }
            } catch (Exception ex) {
                Tools.logException(InternetSlideshows.class, ex, "Could not cache listing: " + url);
            }
        }
        if (PersistentValueManager.isAged(persistentValue)) {
            PersistentValueManager.savePersistentValue(InternetSlideshows.class.getName() + "." + url, content, 60);
        }
        return content;
    }
}

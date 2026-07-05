package com.cipherlink.service;

import com.cipherlink.dto.LinkPreviewDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkPreviewService {

    @Value("${link.preview.timeout-ms:3000}")
    private int timeoutMs;

    private static final Pattern YOUTUBE_PATTERN =
            Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|shorts/))([\\w-]+)");
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<title[^>]*>([^<]*)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_TITLE_PATTERN =
            Pattern.compile("<meta[^>]+property=[\"']og:title[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_DESC_PATTERN =
            Pattern.compile("<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_PATTERN =
            Pattern.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_SITE_PATTERN =
            Pattern.compile("<meta[^>]+property=[\"']og:site_name[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);

    public LinkPreviewDto fetchPreview(String rawUrl) {
        try {
            String url = rawUrl.trim();
            if (!url.startsWith("http")) url = "https://" + url;

            // YouTube shortcut — no network call needed
            Matcher ytMatcher = YOUTUBE_PATTERN.matcher(url);
            if (ytMatcher.find()) {
                String videoId = ytMatcher.group(1);
                return LinkPreviewDto.builder()
                        .url(url)
                        .title("YouTube Video")
                        .imageUrl("https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg")
                        .siteName("YouTube")
                        .faviconUrl("https://www.youtube.com/favicon.ico")
                        .type("youtube")
                        .videoId(videoId)
                        .build();
            }

            // Determine site type from domain
            String domain = URI.create(url).getHost().toLowerCase();
            String siteType = detectSiteType(domain);

            // Fetch HTML
            URL urlObj = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (compatible; CipherLinkBot/1.0; +https://cipherlink.app)");
            conn.setInstanceFollowRedirects(true);

            String html;
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readNBytes(32768); // Read only first 32KB
                html = new String(bytes);
            }
            conn.disconnect();

            String title = extractFirst(OG_TITLE_PATTERN, html);
            if (title == null) title = extractFirst(TITLE_PATTERN, html);

            String description = extractFirst(OG_DESC_PATTERN, html);
            String imageUrl = extractFirst(OG_IMAGE_PATTERN, html);
            String siteName = extractFirst(OG_SITE_PATTERN, html);
            if (siteName == null) siteName = domain.replaceFirst("^www\\.", "");

            return LinkPreviewDto.builder()
                    .url(url)
                    .title(title != null ? title.trim() : domain)
                    .description(description != null ? description.trim() : null)
                    .imageUrl(imageUrl)
                    .siteName(siteName)
                    .faviconUrl("https://www.google.com/s2/favicons?domain=" + domain + "&sz=64")
                    .type(siteType)
                    .build();

        } catch (Exception e) {
            log.warn("Link preview failed for {}: {}", rawUrl, e.getMessage());
            return null;
        }
    }

    private String extractFirst(Pattern pattern, String html) {
        Matcher m = pattern.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    private String detectSiteType(String domain) {
        if (domain.contains("youtube") || domain.contains("youtu.be")) return "youtube";
        if (domain.contains("instagram")) return "instagram";
        if (domain.contains("amazon")) return "amazon";
        if (domain.contains("twitter") || domain.contains("x.com")) return "twitter";
        if (domain.contains("tiktok")) return "tiktok";
        if (domain.contains("spotify")) return "spotify";
        if (domain.contains("github")) return "github";
        return "generic";
    }
}

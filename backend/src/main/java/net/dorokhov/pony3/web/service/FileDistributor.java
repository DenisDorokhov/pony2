package net.dorokhov.pony3.web.service;

import com.google.common.base.Charsets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.dorokhov.pony3.web.dto.FileDistribution;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Based on http://balusc.blogspot.in/2009/02/fileservlet-supporting-resume-and.html.
@Component
public class FileDistributor {

    private static final int BUFFER_SIZE = 20480; // ..bytes = 20KB.
    private static final long EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final String MULTIPART_BYTERANGES = "MULTIPART_BYTERANGES";

    public void distribute(FileDistribution distribution, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(distribution.getFile())) {
            doDistribute(distribution, request, response, inputStream);
        }
    }

    private void doDistribute(FileDistribution distribution, HttpServletRequest request, HttpServletResponse response, FileInputStream inputStream) throws IOException {

        long length = distribution.getFile().length();
        long lastModified = distribution.getModificationDate()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, distribution.getName())) {
            response.setHeader("ETag", createETag(distribution)); // Required in 304.
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setHeader("ETag", createETag(distribution)); // Required in 304.
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, distribution.getName())) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(distribution.getName())) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = subLong(part, 0, part.indexOf("-"));
                    long end = subLong(part, part.indexOf("-") + 1, part.length());
                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }
                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }
                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }

        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set content disposition.
        String disposition = "inline";

        if (!distribution.getMimeType().startsWith("image")) {
            // Else, expect for images, determine content disposition. If content type is supported by
            // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
            String accept = request.getHeader("Accept");
            disposition = accept != null && accepts(accept, distribution.getMimeType())
                    ? "inline" : "attachment";
        }

        // Initialize response.
        response.reset();
        response.setBufferSize(BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + UriUtils.encodeQuery(distribution.getName(), Charsets.UTF_8.name()) + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", createETag(distribution));
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", System.currentTimeMillis() + EXPIRE_TIME);

        // Send requested file (part(s)) to client ------------------------------------------------
        try (
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                ServletOutputStream output = response.getOutputStream()
        ) {
            if (ranges.isEmpty() || ranges.getFirst() == full) {
                // Return full file.
                response.setContentType(distribution.getMimeType());
                response.setHeader("Content-Range", "bytes " + full.start + "-" + full.end + "/" + full.total);
                response.setHeader("Content-Length", String.valueOf(full.length));
                copy(bufferedInputStream, output, length, full.start, full.length);
            } else if (ranges.size() == 1) {
                // Return single part of file.
                Range r = ranges.getFirst();
                response.setContentType(distribution.getMimeType());
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
                // Copy single part range.
                copy(bufferedInputStream, output, length, r.start, r.length);
            } else {
                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BYTERANGES);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
                // Copy multi part range.
                for (Range r : ranges) {
                    // Add multipart boundary and header fields for every range.
                    output.println();
                    output.println("--" + MULTIPART_BYTERANGES);
                    output.println("Content-Type: " + distribution.getMimeType());
                    output.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);
                    // Copy single part range of multi part range.
                    copy(bufferedInputStream, output, length, r.start, r.length);
                }
                // End with multipart boundary.
                output.println();
                output.println("--" + MULTIPART_BYTERANGES + "--");
            }
        }
    }

    private String createETag(FileDistribution distribution) {
        return DigestUtils.md5DigestAsHex(distribution.getFile().getAbsolutePath().getBytes());
    }

    @SuppressFBWarnings("SR_NOT_CHECKED")
    private static void copy(InputStream input, OutputStream output, long inputSize, long start, long length) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        if (inputSize == length) {
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
                output.flush();
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            input.skip(start);
            long toRead = length;
            while ((read = input.read(buffer)) > 0) {
                if ((toRead -= read) > 0) {
                    output.write(buffer, 0, read);
                    output.flush();
                } else {
                    output.write(buffer, 0, (int) toRead + read);
                    output.flush();
                    break;
                }
            }
        }
    }

    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*([,;])\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
                || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
                || Arrays.binarySearch(matchValues, "*") > -1;
    }

    private static long subLong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return !substring.isEmpty() ? Long.parseLong(substring) : -1;
    }

    private static class Range {

        public long start;
        public long end;
        public long length;
        public long total;

        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }
}

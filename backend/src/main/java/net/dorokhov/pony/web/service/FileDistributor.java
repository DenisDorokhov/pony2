package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.domain.FileDistribution;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface FileDistributor {
    void distribute(FileDistribution distribution, HttpServletRequest request, HttpServletResponse response) throws IOException;
}

/***************************************
 * Copyright (c) Intalio, Inc 2010
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
****************************************/
package org.wapama.web.repository.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wapama.web.profile.IDiagramProfile;
import org.wapama.web.profile.IDiagramProfile.IDiagramMarshaller;
import org.wapama.web.repository.IUUIDBasedRepository;


/**
 * @author Antoine Toulme
 * a simple implementation of the UUID repository storing files directly inside the webapp.
 * 
 * Convenient for development.
 */
public class UUIDBasedFileRepository implements IUUIDBasedRepository {

    private static final Logger _logger = LoggerFactory.getLogger(UUIDBasedFileRepository.class);
    
    /**
     * the path to the repository inside the servlet.
     */
    private final static String REPOSITORY_PATH = "repository";

    private String _repositoryPath;
    
    public void configure(HttpServlet servlet) {
        _repositoryPath = servlet.getServletContext().getRealPath("/" + REPOSITORY_PATH);
    }
    
    public byte[] load(HttpServletRequest req, String uuid, IDiagramProfile profile) {
        System.out.println("File loading starts....");
    	String bpmnfile = _repositoryPath + "/" + uuid + ".bpmn";//profile.getSerializedModelExtension();
        String filename = _repositoryPath + "/" + uuid + ".json";
        String myextn = "JSON";
        if (new File(bpmnfile).exists()) {System.out.println("loading bpmn:"+profile.getSerializedModelExtension());
        	myextn = "BPMN";
        	filename = bpmnfile;
        }else if (!new File(filename).exists()) {
           return new byte[0]; // then return nothing. 
        }
        InputStream input = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            input = new FileInputStream(filename);
            byte[] buffer = new byte[4096];
            int read;
           
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            
            
            
			String processxml = new String(buffer, Charset.forName("UTF-8"));
			processxml = processxml.trim();
			String processjson = "";
			//convert 
            if(processxml != null && processxml.length() > 0 && myextn.equals("BPMN")) {
            	System.out.println("processxml="+processxml);
                processjson = profile.createUnmarshaller().parseModel(processxml, profile);
                System.out.println("result json="+processjson.getBytes("UTF-8"));
                return processjson.getBytes("UTF-8");
            } else {
            	//do nothing if its already json
            }
        } catch (FileNotFoundException e) {
            //unlikely since we just checked.
            _logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
            
        } catch (IOException e) {
            _logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (input != null) { try { input.close();} catch(Exception e) {} }
        }
        
        return output.toByteArray();
    }

    public void save(HttpServletRequest req, String uuid, String json, String svg, IDiagramProfile profile, Boolean autosave) {
        String ext = profile.getSerializedModelExtension();
        String model = "";
        try {
            IDiagramMarshaller marshaller = profile.createMarshaller();
            model = marshaller.parseModel(json);
        } catch(Exception e) {
            _logger.error(e.getMessage(), e);
        }
        writeFile(model, _repositoryPath + "/" + uuid + "." + ext);
        writeFile(json, _repositoryPath + "/" + uuid + ".json");
        if (!autosave) {
        	writeFile(svg, _repositoryPath + "/" + uuid + ".svg");
        }
    }
    
    private static void writeFile(String contents, String filename) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            writer.write(contents);
        } catch (IOException e) {
            _logger.error(e.getMessage(), e);
        } finally {
            if (writer != null) { try { writer.close();} catch(Exception e) {} }
        }
    }
    
    public String toXML(String json, IDiagramProfile profile) {
        return profile.createMarshaller().parseModel(json);
    }
}

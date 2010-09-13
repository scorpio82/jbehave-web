package org.jbehave.web.runner.wicket.pages;

import static org.jbehave.web.runner.context.FilesContext.View.RELATIVE_PATH;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.NotImplementedException;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.MultiFileUploadField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.jbehave.web.io.FileManager;
import org.jbehave.web.runner.context.FilesContext;

import com.google.inject.Inject;

public class DataFiles extends Template {

    @Inject
    private FileManager manager;

    private final FilesContext filesContext = new FilesContext();

    public DataFiles() {
        setPageTitle("Data Files");
        setDefaultModel(new CompoundPropertyModel<FilesContext>(filesContext));
        filesContext.setFiles(new ArrayList<File>(manager.list()));
        showContent();
        add(new FileListForm("listForm", filesContext.getFiles()));
        add(new FileContentContainer("contentContainer", filesContext.getContentFilesAsList()));
        add(new FileUploadForm("uploadForm"));
    }

    protected Component pageCompoment(String id) {
        return get(id);
    }

    @SuppressWarnings("serial")
    private class FileListForm extends Form<Void> {
        public FileListForm(String id, List<File> files) {
            super(id);
            add(new CheckBoxMultipleChoice<File>("files", files));
            add(new Button("showContentButton") {
                @Override
                public final void onSubmit() {
                    filesContext.setContentVisible(true);
                    setResponsePage(DataFiles.this);
                }
            });
            add(new Button("hideContentButton") {
                @Override
                public final void onSubmit() {
                    filesContext.setContentVisible(false);
                    setResponsePage(DataFiles.this);
                }
            });
            add(new Button("deleteButton") {
                @Override
                public final void onSubmit() {
                    delete();
                    setResponsePage(DataFiles.class);
                }
            });
        }

    }
    
    public void showContent() {
        Map<String, List<File>> contentFiles = filesContext.getContentFiles();
        contentFiles.clear();
        boolean relativePaths = filesContext.getView() == RELATIVE_PATH ? true : false;
        for (File file : filesContext.getFiles()) {
            List<File> content = manager.listContent(file, relativePaths);
            if (content.size() > 0) {
                contentFiles.put(content.get(0).getPath(), content);
            }
        }
    }

    public void delete() {
        manager.delete(filesContext.getFiles());
    }

    @SuppressWarnings("serial")
    public class FileContentContainer extends WebMarkupContainer {

        public FileContentContainer(String id, List<File> files) {
            super(id);
            add(new ListView<File>("file", files) {

                @Override
                protected void populateItem(ListItem<File> item) {
                    final File file = (File) item.getModelObject();
                    // display the file path
                    item.add(new Label("path", new PropertyModel<File>(item.getDefaultModel(), "path")));
                    item.add(new Link<File>("view") {
                        @Override
                        public void onClick() {
                            setResponsePage(new ViewFileContent(file));
                        }
                    });
                }
            });
        }

        @Override
        public boolean isVisible() {
            return filesContext.getContentVisible();
        }

    }

    @SuppressWarnings("serial")
    private class FileUploadForm extends Form<Void> {
        private final Collection<FileUpload> uploads = new ArrayList<FileUpload>();

        public FileUploadForm(String id) {
            super(id);

            // multi-part uploads
            setMultiPart(true);

            // multi-file upload field
            add(new MultiFileUploadField("uploadInput", new PropertyModel<Collection<FileUpload>>(this, "uploads"), 5));

            // create button used to submit the form
            add(new Button("uploadButton") {
                public void onSubmit() {
                    List<String> errors = filesContext.getErrors();
                    List<File> files = manager.upload(fileItems(uploads), errors);
                    manager.unarchiveFiles(files, errors);
                    setResponsePage(DataFiles.class);
                }

            });

        }

        protected List<FileItem> fileItems(Collection<FileUpload> uploads) {
            List<FileItem>  fileItems = new ArrayList<FileItem>();
            for (FileUpload upload : uploads) {
                fileItems.add(new UploadFileItem(upload));
            }
            return fileItems;
        }

    }

    /**
     * Facade around Wicket's FileUpload backported to more web framework-neutral 
     * Commons FileUpload FileIem, which for some unclear reason was forked to Wicket.
     */
    @SuppressWarnings("serial")
    public static class UploadFileItem implements FileItem {

        private FileUpload upload;
                
        public UploadFileItem(FileUpload upload) {
            this.upload = upload;
        }

        public void delete() {
            upload.delete();            
        }

        public byte[] get() {
            return upload.getBytes();
        }

        public String getContentType() {
            return upload.getContentType();
        }

        public String getFieldName() {
            throw new NotImplementedException();
        }

        public InputStream getInputStream() throws IOException {
            return upload.getInputStream();
        }

        public String getName() {
            return upload.getClientFileName();
        }

        public OutputStream getOutputStream() throws IOException {
            throw new NotImplementedException();
        }

        public long getSize() {
            return upload.getSize();
        }

        public String getString() {
            throw new NotImplementedException();
        }

        public String getString(String encoding) throws UnsupportedEncodingException {
            throw new NotImplementedException();
       }

        public boolean isFormField() {
            throw new NotImplementedException();
        }

        public boolean isInMemory() {
            throw new NotImplementedException();            
        }

        public void setFieldName(String name) {
            throw new NotImplementedException();           
        }

        public void setFormField(boolean state) {
            throw new NotImplementedException();
        }

        public void write(File file) throws Exception {
            upload.writeTo(file);            
        }
        
    }

}

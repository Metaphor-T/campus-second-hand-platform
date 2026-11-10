package com.example.product;

import com.example.exception.BusinessException;
import com.example.service.ProductImageStorageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductImageStorageTest {
    @Test void avatarUsesSeparateOwnerDirectoryAndCleanupCannotDeleteOtherFiles() throws Exception {
        var storage=new ProductImageStorageService(directory.toString());String own=storage.storeAvatar(2,new MockMultipartFile("avatar","a.png","image/png",png()));String other=storage.storeAvatar(3,new MockMultipartFile("avatar","a.png","image/png",png()));
        assertTrue(own.startsWith("/files/avatars/2/"));storage.discardAvatar(2,other);assertTrue(Files.exists(directory.resolve(other.substring(7))));
        storage.discardAvatar(2,own);assertFalse(Files.exists(directory.resolve(own.substring(7))));
    }
    @TempDir Path directory;
    private byte[] png() throws Exception { var output=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",output); return output.toByteArray(); }
    @Test void genuineImageIsStoredUnderOwner() throws Exception { var storage=new ProductImageStorageService(directory.toString()); String path=storage.store(2,new MockMultipartFile("file","a.png","image/png",png())); assertTrue(path.startsWith("/files/products/2/")); assertTrue(Files.isRegularFile(directory.resolve(path.substring(7)))); }
    @Test void renamedScriptCannotMasqueradeAsPng() { var storage=new ProductImageStorageService(directory.toString()); assertThrows(BusinessException.class,()->storage.store(2,new MockMultipartFile("file","a.png","image/png","<script>bad</script>".getBytes()))); }
    @Test void extensionMustAgreeWithContents() throws Exception { var storage=new ProductImageStorageService(directory.toString()); var file=new MockMultipartFile("file","a.jpg","image/jpeg",png()); assertThrows(BusinessException.class,()->storage.store(2,file)); }
    @Test void tooLargeIsRejectedBeforeDecoding() { var storage=new ProductImageStorageService(directory.toString()); var file=new MockMultipartFile("file","a.png","image/png",new byte[(int)ProductImageStorageService.MAX_BYTES+1]); assertThrows(BusinessException.class,()->storage.store(2,file)); }
    @Test void anonymousAndEmptyFilesAreRejected() { var storage=new ProductImageStorageService(directory.toString()); var file=new MockMultipartFile("file","a.png","image/png",new byte[0]); assertThrows(BusinessException.class,()->storage.store(null,file)); assertThrows(BusinessException.class,()->storage.store(2,file)); }
    @Test void oversizedDimensionsAreRejected() throws Exception { var output=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(4097,1,BufferedImage.TYPE_INT_RGB),"png",output); var storage=new ProductImageStorageService(directory.toString()); assertThrows(BusinessException.class,()->storage.store(2,new MockMultipartFile("file","a.png","image/png",output.toByteArray()))); }
}

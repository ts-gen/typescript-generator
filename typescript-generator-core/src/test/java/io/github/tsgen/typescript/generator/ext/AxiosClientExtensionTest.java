
package io.github.tsgen.typescript.generator.ext;

import io.github.tsgen.typescript.generator.Input;
import io.github.tsgen.typescript.generator.JaxrsApplicationTest;
import io.github.tsgen.typescript.generator.RestNamespacing;
import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TestUtils;
import io.github.tsgen.typescript.generator.TypeScriptFileType;
import io.github.tsgen.typescript.generator.TypeScriptGenerator;
import io.github.tsgen.typescript.generator.TypeScriptOutputKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class AxiosClientExtensionTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.generateJaxrsApplicationClient = true;
        settings.restNamespacing = RestNamespacing.perResource;
        settings.extensions.add(new AxiosClientExtension());
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JaxrsApplicationTest.OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;

        Assertions.assertTrue(output.contains("interface Organization"), errorMessage);
        Assertions.assertTrue(output.contains("interface Address"), errorMessage);
        Assertions.assertTrue(output.contains("interface Person"), errorMessage);
        Assertions.assertTrue(output.contains("interface HttpClient"), errorMessage);

        Assertions.assertTrue(output.contains("class OrganizationsResourceClient<O>"), errorMessage);
        Assertions.assertTrue(output.contains("class PersonResourceClient<O>"), errorMessage);
        Assertions.assertTrue(output.contains("type RestResponse<R> = Promise<Axios.GenericAxiosResponse<R>>"), errorMessage);

        Assertions.assertTrue(output.contains("class AxiosHttpClient implements HttpClient<Axios.AxiosRequestConfig>"), errorMessage);
        Assertions.assertTrue(output.contains("request<R>(requestConfig: { method: string; url: string; queryParams?: any; data?: any; copyFn?: (data: R) => R; options?: Axios.AxiosRequestConfig; }): RestResponse<R>"), errorMessage);
        Assertions.assertTrue(output.contains("class AxiosOrganizationsResourceClient extends OrganizationsResourceClient<Axios.AxiosRequestConfig>"), errorMessage);
        Assertions.assertTrue(output.contains("class AxiosPersonResourceClient extends PersonResourceClient<Axios.AxiosRequestConfig>"), errorMessage);
        Assertions.assertTrue(output.contains("constructor(baseURL: string, axiosInstance: Axios.AxiosInstance = axios.create())"), errorMessage);
    }

    @Test
    public void mapPackagesToNamespaces() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.generateJaxrsApplicationClient = true;
        settings.restNamespacing = RestNamespacing.perResource;
        settings.mapPackagesToNamespaces = true;
        settings.extensions.add(new AxiosClientExtension());
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JaxrsApplicationTest.OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;

        Assertions.assertTrue(output.contains("interface Organization"), errorMessage);
        Assertions.assertTrue(output.contains("interface Address"), errorMessage);
        Assertions.assertTrue(output.contains("interface Person"), errorMessage);
        Assertions.assertTrue(output.contains("interface HttpClient"), errorMessage);

        Assertions.assertTrue(output.contains("class OrganizationsResourceClient<O>"), errorMessage);
        Assertions.assertTrue(output.contains("class PersonResourceClient<O>"), errorMessage);
        Assertions.assertTrue(output.contains("type RestResponse<R> = Promise<Axios.GenericAxiosResponse<R>>"), errorMessage);

        Assertions.assertTrue(output.contains("class AxiosHttpClient implements HttpClient<Axios.AxiosRequestConfig>"), errorMessage);
        Assertions.assertTrue(output.contains("request<R>(requestConfig: { method: string; url: string; queryParams?: any; data?: any; copyFn?: (data: R) => R; options?: Axios.AxiosRequestConfig; }): RestResponse<R>"), errorMessage);
        Assertions.assertTrue(output.contains("export class AxiosOrganizationsResourceClient extends io.github.tsgen.typescript.generator.JaxrsApplicationTest.OrganizationsResourceClient<Axios.AxiosRequestConfig>"), errorMessage);
        Assertions.assertTrue(output.contains("class AxiosPersonResourceClient extends io.github.tsgen.typescript.generator.JaxrsApplicationTest.PersonResourceClient<Axios.AxiosRequestConfig>"), errorMessage);
        Assertions.assertTrue(output.contains("constructor(baseURL: string, axiosInstance: Axios.AxiosInstance = axios.create())"), errorMessage);
    }
}

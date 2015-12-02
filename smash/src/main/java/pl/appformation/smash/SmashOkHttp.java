/*
 * Copyright (C) 2015 Appformation sp. z o.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.appformation.smash;

import android.support.annotation.NonNull;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.IOException;
import okio.Buffer;
import okio.BufferedSource;
import pl.appformation.smash.errors.SmashError;
import static pl.appformation.smash.SmashRequest.Method.DELETE;
import static pl.appformation.smash.SmashRequest.Method.GET;
import static pl.appformation.smash.SmashRequest.Method.HEAD;
import static pl.appformation.smash.SmashRequest.Method.PATCH;
import static pl.appformation.smash.SmashRequest.Method.POST;
import static pl.appformation.smash.SmashRequest.Method.PUT;

public class SmashOkHttp
{

    /** User-Agent header name */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /** Default OkHttpClient instance */
    private static OkHttpClient sHttpClient = new OkHttpClient();

    /**
     * Adds {@link Interceptor} object to default {@link OkHttpClient} instance.
     *
     * @param interceptor Interceptor object
     */
    public static void addNetworkInterceptor(@NonNull Interceptor interceptor)
    {
        sHttpClient.networkInterceptors().add(interceptor);
    }

    private static RequestBody convertBody(SmashRequest request, BufferedSource body) throws SmashError
    {
        if (body == null && request.getMethod() == DELETE)
        {
            return RequestBody.create(null, new byte[0]);
        }

        try
        {
            if (body == null)
            {
                body = new Buffer();
            }

            return RequestBody.create(MediaType.parse(request.getBodyContentType()), body.readUtf8());
        }
        catch (IOException ioe)
        {
            throw new SmashError(ioe);
        }
    }

    private static BufferedSource getBody(SmashRequest request)
    {
        if (request.getMethod() == GET || request.getMethod() == HEAD)
        {
            return null;
        }

        return request.getBody();
    }

    static @NonNull SmashNetworkData perform(SmashRequest<?> request) throws SmashError
    {
        SmashNetworkData data = new SmashNetworkData();

        try
        {
            Request.Builder okBuilder = new Request.Builder().url(request.getUrl());
            okBuilder.addHeader(HEADER_USER_AGENT, Smash.getUserAgent());

            Headers requestHeaders = request.getHeaders();
            if (requestHeaders != null)
            {
                okBuilder.headers(requestHeaders);
                if (!requestHeaders.names().contains(HEADER_USER_AGENT))
                {
                    okBuilder.addHeader(HEADER_USER_AGENT, Smash.getUserAgent());
                }
            }

            BufferedSource body = getBody(request);
            switch (request.getMethod())
            {
                case GET:
                {
                    okBuilder = okBuilder.get();
                    break;
                }
                case POST:
                {
                    okBuilder = okBuilder.post(convertBody(request, body));
                    break;
                }
                case PUT:
                {
                    okBuilder = okBuilder.put(convertBody(request, body));
                    break;
                }
                case DELETE:
                {
                    okBuilder = okBuilder.delete(convertBody(request, body));
                    break;
                }
                case HEAD:
                {
                    okBuilder = okBuilder.head();
                    break;
                }
                case PATCH:
                {
                    okBuilder = okBuilder.patch(convertBody(request, body));
                    break;
                }
            }

            Request okRequest = okBuilder.build();
            Response okResponse = sHttpClient.newCall(okRequest).execute();

            if (body != null)
            {
                body.close();
            }

            data.code = okResponse.code();
            data.headers = okResponse.headers();
            data.source = okResponse.body().source();
            data.length = okResponse.body().contentLength();
        }
        catch (IOException ioe)
        {
            throw new SmashError(ioe);
        }

        return data;
    }

    /**
     * Removes {@link Interceptor} object from default {@link OkHttpClient} instance.
     *
     * @param interceptor Interceptor object
     */
    public static void removeNetworkInterceptor(@NonNull Interceptor interceptor)
    {
        sHttpClient.networkInterceptors().remove(interceptor);
    }

}

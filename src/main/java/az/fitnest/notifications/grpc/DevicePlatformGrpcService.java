package az.fitnest.notifications.grpc;

import az.fitnest.notifications.service.DevicePlatformService;
import az.fitnest.notifications.grpc.GetUserPlatformRequest;
import az.fitnest.notifications.grpc.GetUserPlatformResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DevicePlatformGrpcService extends DevicePlatformServiceGrpc.DevicePlatformServiceImplBase {
    @Autowired
    private DevicePlatformService devicePlatformService;

    @Override
    public void getUserPlatform(GetUserPlatformRequest request, StreamObserver<GetUserPlatformResponse> responseObserver) {
        String platform = devicePlatformService.getUserPlatform(request.getUserId());
        GetUserPlatformResponse response = GetUserPlatformResponse.newBuilder()
                .setPlatform(platform)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

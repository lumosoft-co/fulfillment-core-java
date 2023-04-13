import com.agoramp.AgoraFulfillmentService;
import com.agoramp.FulfillmentExecutor;
import com.agoramp.data.FulfillmentDestinationConfig;
import com.agoramp.data.models.fulfillments.GameServerCommandsFulfillment;
import com.agoramp.error.ServiceAlreadyInitializedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CLIServiceTest {

    public static void main(String[] args) throws ServiceAlreadyInitializedException {
        String key = args[0];
        AgoraFulfillmentService.INSTANCE.initialize(new FulfillmentDestinationConfig(key), new FulfillmentExecutor() {
            @Override
            public Flux<String> retrieveOnlinePlayerIds() {
                return Mono.just("Justin42069").flux();
            }

            @Override
            public Mono<Boolean> processCommandFulfillment(GameServerCommandsFulfillment fulfillment) {
                return Mono.fromSupplier(() -> {
                    System.out.println(fulfillment);
                    for (GameServerCommandsFulfillment.Command command : fulfillment.getCommands()) {
                        System.out.println(command.getCommand());
                    }
                    return false;
                });
            }
        });
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}